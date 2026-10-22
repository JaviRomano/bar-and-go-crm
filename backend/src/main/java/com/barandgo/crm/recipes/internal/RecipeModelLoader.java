package com.barandgo.crm.recipes.internal;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.barandgo.crm.masterdata.Allergen;
import com.barandgo.crm.masterdata.UnitConverter;
import com.barandgo.crm.masterdata.UnitOfMeasureDTO;
import com.barandgo.crm.masterdata.UnitOfMeasureService;
import com.barandgo.crm.purchasing.ReferenceCostDTO;
import com.barandgo.crm.purchasing.StockItemDTO;
import com.barandgo.crm.purchasing.StockItemService;
import com.barandgo.crm.purchasing.StockItemUnits;
import com.barandgo.crm.recipes.internal.RecipeCosting.ItemInfo;
import com.barandgo.crm.recipes.internal.RecipeCosting.LineSpec;
import com.barandgo.crm.recipes.internal.RecipeCosting.RecipeSpec;

/**
 * Carga escandallos y artículos y los traduce a las estructuras de {@link RecipeCosting}. Sin costes
 * (para alérgenos o consumo) evita consultar la tarifa de cada artículo.
 */
@Component
class RecipeModelLoader {

    record Model(RecipeCosting costing, Map<Long, Recipe> recipes, Map<Long, StockItemDTO> items,
                 Map<Long, String> unitCodes, Map<String, Long> unitIds, UnitConverter generic) {

        String unitCode(Long unitId) {
            return unitCodes.get(unitId);
        }

        UnitConverter converterOf(StockItemDTO item) {
            return StockItemUnits.converterFor(item, generic);
        }

        /** Conversor para la unidad de rendimiento: con las reglas del artículo si la elaboración se guarda. */
        UnitConverter outputConverterOf(Recipe recipe) {
            return recipe.getStockItemId() != null ? converterOf(items.get(recipe.getStockItemId())) : generic;
        }
    }

    private final RecipeRepository recipeRepository;
    private final StockItemService stockItemService;
    private final UnitOfMeasureService unitService;

    RecipeModelLoader(RecipeRepository recipeRepository, StockItemService stockItemService,
                      UnitOfMeasureService unitService) {
        this.recipeRepository = recipeRepository;
        this.stockItemService = stockItemService;
        this.unitService = unitService;
    }

    Model load(boolean withCosts) {
        UnitConverter generic = unitService.genericConverter();
        List<UnitOfMeasureDTO> units = unitService.getUnits();
        Map<Long, String> unitCodes = units.stream().collect(Collectors.toMap(UnitOfMeasureDTO::id, UnitOfMeasureDTO::code));
        Map<String, Long> unitIds = units.stream().collect(Collectors.toMap(UnitOfMeasureDTO::code, UnitOfMeasureDTO::id));
        Map<Long, StockItemDTO> items = stockItemService.getStockItems(null, null).stream()
                .collect(Collectors.toMap(StockItemDTO::id, Function.identity()));
        Map<Long, Recipe> recipes = recipeRepository.findAll().stream()
                .collect(Collectors.toMap(Recipe::getId, Function.identity()));

        Set<Long> usedItems = new HashSet<>();
        recipes.values().forEach(recipe -> {
            if (recipe.getStockItemId() != null) {
                usedItems.add(recipe.getStockItemId());
            }
            recipe.getLines().stream().map(RecipeLine::getStockItemId).filter(id -> id != null).forEach(usedItems::add);
        });

        Map<Long, ItemInfo> itemInfos = new HashMap<>();
        for (Long itemId : usedItems) {
            StockItemDTO item = items.get(itemId);
            BigDecimal cost = withCosts
                    ? stockItemService.getReferenceCost(itemId).map(ReferenceCostDTO::costPerStockUnit).orElse(null)
                    : null;
            Set<Allergen> allergens = item.allergens() == null || item.allergens().isEmpty()
                    ? EnumSet.noneOf(Allergen.class)
                    : EnumSet.copyOf(item.allergens());
            itemInfos.put(itemId, new ItemInfo(itemId, item.code(), item.stockUnit(),
                    StockItemUnits.converterFor(item, generic), cost, allergens));
        }

        Model partial = new Model(null, recipes, items, unitCodes, unitIds, generic);
        Map<Long, RecipeSpec> specs = new HashMap<>();
        for (Recipe recipe : recipes.values()) {
            List<LineSpec> lines = recipe.getLines().stream()
                    .map(line -> new LineSpec(line.getStockItemId(),
                            line.getSubRecipe() != null ? line.getSubRecipe().getId() : null,
                            line.getGrossQuantity(), unitCodes.get(line.getUnitId()), line.getYieldPct()))
                    .toList();
            specs.put(recipe.getId(), new RecipeSpec(recipe.getId(), recipe.getName(), recipe.getType(),
                    recipe.outputQuantity(), unitCodes.get(recipe.getYieldUnitId()), partial.outputConverterOf(recipe),
                    recipe.getStockItemId(), lines));
        }
        return new Model(new RecipeCosting(itemInfos, specs), recipes, items, unitCodes, unitIds, generic);
    }
}
