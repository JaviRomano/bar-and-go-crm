package com.barandgo.crm.recipes.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.barandgo.crm.masterdata.Allergen;
import com.barandgo.crm.masterdata.UnitConverter;
import com.barandgo.crm.recipes.RecipeType;

/**
 * Coste, alérgenos y consumo de un escandallo. Sin dependencias de persistencia: recibe artículos y
 * escandallos ya cargados, y se prueba con valores directos.
 *
 * <ul>
 *   <li>Cantidad neta = bruta x rendimiento. El coste se calcula sobre la bruta: lo que se compra
 *       incluye lo que se pierde al limpiar.</li>
 *   <li>Coste de una línea de artículo: cantidad bruta en unidad de inventario x coste por unidad de
 *       inventario. De un sub-escandallo: cantidad en su unidad de rendimiento x su coste por unidad.</li>
 *   <li>Un artículo sin coste conocido no suma: el resultado queda marcado como incompleto con la
 *       lista de artículos que faltan, en lugar de dar un coste falso.</li>
 *   <li>Alérgenos: unión de los de todos los ingredientes, sub-escandallos incluidos.</li>
 * </ul>
 */
final class RecipeCosting {

    static final int QUANTITY_SCALE = 4;
    static final int COST_SCALE = 6;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** Artículo con su coste de reposición por unidad de inventario (null si no se conoce). */
    record ItemInfo(Long id, String code, String stockUnit, UnitConverter converter, BigDecimal costPerStockUnit,
                    Set<Allergen> allergens) {
    }

    /** Línea: exactamente uno de stockItemId o subRecipeId. */
    record LineSpec(Long stockItemId, Long subRecipeId, BigDecimal grossQuantity, String unit, BigDecimal yieldPct) {
    }

    /**
     * @param outputQuantity   raciones (DISH) o cantidad que rinde (PREPARATION)
     * @param outputUnit       unidad de rendimiento (PREPARATION); null en DISH
     * @param outputConverter  conversor para la unidad de rendimiento (con las reglas del artículo de stock
     *                         si la elaboración se guarda)
     * @param stockItemId      artículo de stock de una elaboración inventariable; null si no lo es
     */
    record RecipeSpec(Long id, String name, RecipeType type, BigDecimal outputQuantity, String outputUnit,
                      UnitConverter outputConverter, Long stockItemId, List<LineSpec> lines) {
    }

    record LineCost(BigDecimal netQuantity, BigDecimal unitCost, BigDecimal cost) {
    }

    /**
     * @param costPerOutputUnit coste por ración (DISH) o por unidad de rendimiento (PREPARATION)
     */
    record RecipeCost(List<LineCost> lines, BigDecimal totalCost, BigDecimal costPerOutputUnit, boolean complete,
                      Set<Long> missingItemIds, Set<Allergen> allergens) {
    }

    private final Map<Long, ItemInfo> items;
    private final Map<Long, RecipeSpec> recipes;
    private final Map<Long, RecipeCost> computed = new HashMap<>();
    private final Set<Long> inProgress = new HashSet<>();

    RecipeCosting(Map<Long, ItemInfo> items, Map<Long, RecipeSpec> recipes) {
        this.items = items;
        this.recipes = recipes;
    }

    RecipeCost cost(Long recipeId) {
        RecipeCost cached = computed.get(recipeId);
        if (cached != null) {
            return cached;
        }
        if (!inProgress.add(recipeId)) {
            throw new IllegalStateException("Ciclo de sub-escandallos en el escandallo " + recipeId);
        }
        RecipeSpec recipe = recipe(recipeId);

        List<LineCost> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        boolean complete = true;
        Set<Long> missing = new LinkedHashSet<>();
        Set<Allergen> allergens = EnumSet.noneOf(Allergen.class);

        for (LineSpec line : recipe.lines()) {
            BigDecimal net = line.grossQuantity().multiply(line.yieldPct())
                    .divide(HUNDRED, QUANTITY_SCALE, RoundingMode.HALF_UP);
            BigDecimal unitCost;
            if (line.stockItemId() != null) {
                ItemInfo item = item(line.stockItemId());
                allergens.addAll(item.allergens());
                if (item.costPerStockUnit() == null) {
                    complete = false;
                    missing.add(item.id());
                    lines.add(new LineCost(net, null, null));
                    continue;
                }
                unitCost = item.costPerStockUnit().multiply(item.converter().factor(line.unit(), item.stockUnit()));
            } else {
                RecipeSpec sub = recipe(line.subRecipeId());
                RecipeCost subCost = cost(sub.id());
                allergens.addAll(subCost.allergens());
                missing.addAll(subCost.missingItemIds());
                complete &= subCost.complete();
                unitCost = subCost.costPerOutputUnit().multiply(sub.outputConverter().factor(line.unit(), sub.outputUnit()));
            }
            BigDecimal lineCost = line.grossQuantity().multiply(unitCost).setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
            lines.add(new LineCost(net, unitCost.setScale(COST_SCALE, RoundingMode.HALF_UP), lineCost));
            total = total.add(lineCost);
        }

        BigDecimal perOutput = total.divide(recipe.outputQuantity(), COST_SCALE, RoundingMode.HALF_UP);
        RecipeCost result = new RecipeCost(lines, total.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP), perOutput,
                complete, missing, allergens);
        inProgress.remove(recipeId);
        computed.put(recipeId, result);
        return result;
    }

    /**
     * Artículos de stock que consume producir (o vender) {@code outputQuantity} unidades del escandallo,
     * en unidad de inventario. Un sub-escandallo inventariable se consume de su stock (se elaboró antes);
     * uno no inventariable se descompone en sus ingredientes (D13).
     */
    Map<Long, BigDecimal> stockConsumption(Long recipeId, BigDecimal outputQuantity) {
        Map<Long, BigDecimal> consumption = new HashMap<>();
        explode(recipe(recipeId), outputQuantity, consumption, new HashSet<>());
        consumption.replaceAll((id, quantity) -> quantity.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP));
        return consumption;
    }

    private void explode(RecipeSpec recipe, BigDecimal outputQuantity, Map<Long, BigDecimal> consumption,
                         Set<Long> path) {
        if (!path.add(recipe.id())) {
            throw new IllegalStateException("Ciclo de sub-escandallos en el escandallo " + recipe.id());
        }
        BigDecimal factor = outputQuantity.divide(recipe.outputQuantity(), 12, RoundingMode.HALF_UP);
        for (LineSpec line : recipe.lines()) {
            BigDecimal needed = line.grossQuantity().multiply(factor);
            if (line.stockItemId() != null) {
                ItemInfo item = item(line.stockItemId());
                consumption.merge(item.id(), item.converter().convert(needed, line.unit(), item.stockUnit()),
                        BigDecimal::add);
                continue;
            }
            RecipeSpec sub = recipe(line.subRecipeId());
            BigDecimal inSubUnit = sub.outputConverter().convert(needed, line.unit(), sub.outputUnit());
            if (sub.stockItemId() != null) {
                ItemInfo stored = item(sub.stockItemId());
                consumption.merge(stored.id(),
                        stored.converter().convert(inSubUnit, sub.outputUnit(), stored.stockUnit()), BigDecimal::add);
            } else {
                explode(sub, inSubUnit, consumption, path);
            }
        }
        path.remove(recipe.id());
    }

    private RecipeSpec recipe(Long id) {
        RecipeSpec recipe = recipes.get(id);
        if (recipe == null) {
            throw new IllegalStateException("Escandallo no cargado: " + id);
        }
        return recipe;
    }

    private ItemInfo item(Long id) {
        ItemInfo item = items.get(id);
        if (item == null) {
            throw new IllegalStateException("Artículo no cargado: " + id);
        }
        return item;
    }
}
