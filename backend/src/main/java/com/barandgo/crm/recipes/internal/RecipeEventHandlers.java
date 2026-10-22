package com.barandgo.crm.recipes.internal;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.barandgo.crm.catalog.ProductAllergenSource;
import com.barandgo.crm.masterdata.Allergen;
import com.barandgo.crm.purchasing.StockItemCostChanged;

/**
 * Lo que escandallos aporta a otros módulos sin que estos dependan de él.
 */
final class RecipeEventHandlers {

    private RecipeEventHandlers() {
    }

    /**
     * Recalcula el coste de los escandallos afectados cuando compras avisa de un cambio de coste.
     * Asíncrono, después del commit de compras y en su propia transacción: un fallo aquí no deshace la
     * recepción o la tarifa que lo provocó.
     */
    @Component
    static class CostChangeListener {

        private final RecipeServiceImpl recipeService;

        CostChangeListener(RecipeServiceImpl recipeService) {
            this.recipeService = recipeService;
        }

        @ApplicationModuleListener
        public void on(StockItemCostChanged event) {
            recipeService.recalculateAffectedBy(event.stockItemId());
        }
    }

    /** Alérgenos de cada producto de la carta, desde los ingredientes de su escandallo. */
    @Component
    static class AllergenSource implements ProductAllergenSource {

        private final RecipeRepository recipeRepository;
        private final RecipeModelLoader modelLoader;

        AllergenSource(RecipeRepository recipeRepository, RecipeModelLoader modelLoader) {
            this.recipeRepository = recipeRepository;
            this.modelLoader = modelLoader;
        }

        @Override
        public Map<Long, Set<Allergen>> allergensByProduct(Collection<Long> productIds) {
            if (productIds.isEmpty()) {
                return Map.of();
            }
            List<Recipe> recipes = recipeRepository.findByProductIdIn(productIds);
            if (recipes.isEmpty()) {
                return Map.of();
            }
            RecipeModelLoader.Model model = modelLoader.load(false);
            return recipes.stream().collect(Collectors.toMap(Recipe::getProductId, recipe -> {
                Set<Allergen> allergens = model.costing().cost(recipe.getId()).allergens();
                return allergens.isEmpty() ? EnumSet.noneOf(Allergen.class) : EnumSet.copyOf(allergens);
            }));
        }
    }
}
