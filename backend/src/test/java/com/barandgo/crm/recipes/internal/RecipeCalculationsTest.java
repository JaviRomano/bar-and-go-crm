package com.barandgo.crm.recipes.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.barandgo.crm.masterdata.Allergen;
import com.barandgo.crm.masterdata.UnitConverter;
import com.barandgo.crm.recipes.RecipeType;
import com.barandgo.crm.recipes.internal.RecipeCosting.ItemInfo;
import com.barandgo.crm.recipes.internal.RecipeCosting.LineSpec;
import com.barandgo.crm.recipes.internal.RecipeCosting.RecipeCost;
import com.barandgo.crm.recipes.internal.RecipeCosting.RecipeSpec;

class RecipeCalculationsTest {

    private static final UnitConverter GENERIC = new UnitConverter(List.of(
            new UnitConverter.Rule("kg", "g", new BigDecimal("1000")),
            new UnitConverter.Rule("l", "ml", new BigDecimal("1000"))));

    // Ternera a 10 €/kg; patata a 1 €/kg; tomate a 2 €/kg; aceite a 8 €/l (GLUTEN para probar la unión)
    private static final ItemInfo BEEF = item(1L, "kg", "10.00", Set.of());
    private static final ItemInfo POTATO = item(2L, "kg", "1.00", Set.of());
    private static final ItemInfo TOMATO = item(3L, "kg", "2.00", Set.of(Allergen.CELERY));
    private static final ItemInfo OIL = item(4L, "l", "8.00", Set.of());
    private static final ItemInfo FLOUR = item(5L, "kg", null, Set.of(Allergen.GLUTEN));
    private static final ItemInfo SAUCE_STOCK = item(6L, "kg", "3.00", Set.of());

    @Nested
    class Costing {

        @Test
        void costUsesGrossQuantityAndNetIsAfterYield() {
            // 250 g de patata con rendimiento del 80 %: se compran 0,25 kg, al plato llegan 200 g
            RecipeSpec fries = dish(10L, "1", line(POTATO, "250", "g", "80"));

            RecipeCost cost = costing(List.of(fries)).cost(10L);

            assertThat(cost.lines().get(0).netQuantity()).isEqualByComparingTo("200");
            assertThat(cost.lines().get(0).cost()).isEqualByComparingTo("0.25");
            assertThat(cost.lines().get(0).unitCost()).isEqualByComparingTo("0.001");  // €/g
            assertThat(cost.totalCost()).isEqualByComparingTo("0.25");
        }

        @Test
        void costPerPortionDividesByPortions() {
            // Bandeja para 4: 1 kg de ternera (10 €) + 30 ml de aceite (0,24 €)
            RecipeSpec tray = dish(10L, "4", line(BEEF, "1", "kg", "100"), line(OIL, "30", "ml", "100"));

            RecipeCost cost = costing(List.of(tray)).cost(10L);

            assertThat(cost.totalCost()).isEqualByComparingTo("10.24");
            assertThat(cost.costPerOutputUnit()).isEqualByComparingTo("2.56");
            assertThat(cost.complete()).isTrue();
        }

        @Test
        void subRecipeCostsPerUnitOfItsYield() {
            // Salsa: 2,2 kg de tomate (4,40 €) + 100 ml de aceite (0,80 €) rinden 2 kg -> 2,60 €/kg
            RecipeSpec sauce = preparation(20L, "2", "kg", null,
                    line(TOMATO, "2.2", "kg", "100"), line(OIL, "100", "ml", "100"));
            // Plato con 150 g de salsa -> 0,39 €
            RecipeSpec dish = dish(10L, "1", subLine(20L, "150", "g"));

            RecipeCosting costing = costing(List.of(sauce, dish));

            assertThat(costing.cost(20L).costPerOutputUnit()).isEqualByComparingTo("2.60");
            assertThat(costing.cost(10L).totalCost()).isEqualByComparingTo("0.39");
        }

        @Test
        void allergensPropagateFromSubRecipes() {
            RecipeSpec sauce = preparation(20L, "1", "kg", null, line(TOMATO, "1", "kg", "100"));
            RecipeSpec pizza = dish(10L, "1", line(FLOUR, "200", "g", "100"), subLine(20L, "100", "g"));

            assertThat(costing(List.of(sauce, pizza)).cost(10L).allergens())
                    .containsExactlyInAnyOrder(Allergen.GLUTEN, Allergen.CELERY);
        }

        @Test
        void itemWithoutCostMarksRecipeIncompleteInsteadOfInventingIt() {
            RecipeSpec bread = dish(10L, "1", line(FLOUR, "300", "g", "100"), line(OIL, "10", "ml", "100"));

            RecipeCost cost = costing(List.of(bread)).cost(10L);

            assertThat(cost.complete()).isFalse();
            assertThat(cost.missingItemIds()).containsExactly(FLOUR.id());
            assertThat(cost.totalCost()).isEqualByComparingTo("0.08");  // solo el aceite
            assertThat(cost.lines().get(0).cost()).isNull();
        }

        @Test
        void incompleteSubRecipeMakesParentIncomplete() {
            RecipeSpec dough = preparation(20L, "1", "kg", null, line(FLOUR, "1", "kg", "100"));
            RecipeSpec pizza = dish(10L, "1", subLine(20L, "250", "g"));

            RecipeCost cost = costing(List.of(dough, pizza)).cost(10L);

            assertThat(cost.complete()).isFalse();
            assertThat(cost.missingItemIds()).containsExactly(FLOUR.id());
        }

        @Test
        void cycleIsDetectedInsteadOfLoopingForever() {
            RecipeSpec a = preparation(20L, "1", "kg", null, subLine(21L, "1", "kg"));
            RecipeSpec b = preparation(21L, "1", "kg", null, subLine(20L, "1", "kg"));

            assertThatThrownBy(() -> costing(List.of(a, b)).cost(20L)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class Consumption {

        @Test
        void producingScalesIngredientsToOutput() {
            // Salsa para 2 kg; se producen 3 kg -> 3,3 kg de tomate y 150 ml (0,15 l) de aceite
            RecipeSpec sauce = preparation(20L, "2", "kg", 6L,
                    line(TOMATO, "2.2", "kg", "100"), line(OIL, "100", "ml", "100"));

            Map<Long, BigDecimal> consumption = costing(List.of(sauce)).stockConsumption(20L, new BigDecimal("3"));

            assertThat(consumption.get(TOMATO.id())).isEqualByComparingTo("3.3");
            assertThat(consumption.get(OIL.id())).isEqualByComparingTo("0.15");
        }

        @Test
        void storedSubRecipeIsConsumedFromItsStock() {
            // La salsa es inventariable (artículo 6): el plato consume salsa, no tomate
            RecipeSpec sauce = preparation(20L, "2", "kg", SAUCE_STOCK.id(), line(TOMATO, "2.2", "kg", "100"));
            RecipeSpec dish = dish(10L, "1", subLine(20L, "150", "g"), line(POTATO, "200", "g", "80"));

            Map<Long, BigDecimal> consumption = costing(List.of(sauce, dish)).stockConsumption(10L, new BigDecimal("2"));

            assertThat(consumption).containsOnlyKeys(SAUCE_STOCK.id(), POTATO.id());
            assertThat(consumption.get(SAUCE_STOCK.id())).isEqualByComparingTo("0.3");
            assertThat(consumption.get(POTATO.id())).isEqualByComparingTo("0.4");  // bruto, no neto
        }

        @Test
        void nonStoredSubRecipeIsExplodedIntoIngredients() {
            RecipeSpec dressing = preparation(20L, "100", "ml", null,
                    line(OIL, "80", "ml", "100"), line(TOMATO, "20", "g", "100"));
            RecipeSpec salad = dish(10L, "1", subLine(20L, "50", "ml"));

            Map<Long, BigDecimal> consumption = costing(List.of(dressing, salad)).stockConsumption(10L, BigDecimal.ONE);

            assertThat(consumption.get(OIL.id())).isEqualByComparingTo("0.04");     // 40 ml
            assertThat(consumption.get(TOMATO.id())).isEqualByComparingTo("0.01");  // 10 g
        }
    }

    @Nested
    class Cycles {

        @Test
        void directSelfReferenceIsACycle() {
            assertThat(RecipeGraph.findCycle(Map.of(1L, Set.of(1L)), 1L)).contains(List.of(1L, 1L));
        }

        @Test
        void indirectCycleReturnsThePath() {
            Map<Long, Set<Long>> edges = Map.of(1L, Set.of(2L), 2L, Set.of(3L), 3L, Set.of(1L));

            assertThat(RecipeGraph.findCycle(edges, 1L)).contains(List.of(1L, 2L, 3L, 1L));
        }

        @Test
        void sharedSubRecipeIsNotACycle() {
            // Dos ramas que usan la misma salsa (diamante): no hay ciclo
            Map<Long, Set<Long>> edges = Map.of(1L, Set.of(2L, 3L), 2L, Set.of(4L), 3L, Set.of(4L));

            assertThat(RecipeGraph.findCycle(edges, 1L)).isEmpty();
        }

        @Test
        void cycleElsewhereDoesNotConcernStart() {
            Map<Long, Set<Long>> edges = Map.of(1L, Set.of(2L), 2L, Set.of(3L), 3L, Set.of(2L));

            assertThat(RecipeGraph.findCycle(edges, 1L)).isEmpty();
        }
    }

    @Nested
    class Pricing {

        @Test
        void foodCostIsOverPriceWithoutVat() {
            // PVP 11,00 con 10 % de IVA -> 10,00 sin IVA; coste 3,20 -> 32 %
            assertThat(MenuPricing.foodCostPct(new BigDecimal("3.20"), new BigDecimal("11.00"), new BigDecimal("10")))
                    .isEqualByComparingTo("32.00");
        }

        @Test
        void grossMarginIsNetPriceMinusCost() {
            assertThat(MenuPricing.grossMargin(new BigDecimal("3.20"), new BigDecimal("11.00"), new BigDecimal("10")))
                    .isEqualByComparingTo("6.80");
        }

        @Test
        void suggestedPriceReachesTargetFoodCostIncludingVat() {
            // 3,00 € de coste al 30 % -> 10,00 sin IVA -> 11,00 con 10 %
            assertThat(MenuPricing.suggestedPrice(new BigDecimal("3.00"), new BigDecimal("30"), new BigDecimal("10")))
                    .isEqualByComparingTo("11.00");
        }

        @Test
        void freeProductHasNoFoodCost() {
            assertThat(MenuPricing.foodCostPct(BigDecimal.ONE, BigDecimal.ZERO, new BigDecimal("10"))).isNull();
        }
    }

    private static RecipeCosting costing(List<RecipeSpec> recipes) {
        return new RecipeCosting(
                Map.of(BEEF.id(), BEEF, POTATO.id(), POTATO, TOMATO.id(), TOMATO, OIL.id(), OIL,
                        FLOUR.id(), FLOUR, SAUCE_STOCK.id(), SAUCE_STOCK),
                recipes.stream().collect(Collectors.toMap(RecipeSpec::id, r -> r)));
    }

    private static ItemInfo item(Long id, String stockUnit, String cost, Set<Allergen> allergens) {
        return new ItemInfo(id, "ITEM-" + id, stockUnit, GENERIC, cost == null ? null : new BigDecimal(cost), allergens);
    }

    private static LineSpec line(ItemInfo item, String gross, String unit, String yieldPct) {
        return new LineSpec(item.id(), null, new BigDecimal(gross), unit, new BigDecimal(yieldPct));
    }

    private static LineSpec subLine(Long recipeId, String gross, String unit) {
        return new LineSpec(null, recipeId, new BigDecimal(gross), unit, new BigDecimal("100"));
    }

    private static RecipeSpec dish(Long id, String portions, LineSpec... lines) {
        return new RecipeSpec(id, "Plato " + id, RecipeType.DISH, new BigDecimal(portions), null, GENERIC, null,
                List.of(lines));
    }

    private static RecipeSpec preparation(Long id, String yield, String unit, Long stockItemId, LineSpec... lines) {
        return new RecipeSpec(id, "Elaboración " + id, RecipeType.PREPARATION, new BigDecimal(yield), unit, GENERIC,
                stockItemId, List.of(lines));
    }
}
