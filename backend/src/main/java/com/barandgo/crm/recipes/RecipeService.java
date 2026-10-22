package com.barandgo.crm.recipes;

import java.util.List;

import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.recipes.RecipeDTOs.CostHistoryEntry;
import com.barandgo.crm.recipes.RecipeDTOs.ProductionDTO;
import com.barandgo.crm.recipes.RecipeDTOs.ProductionRequest;
import com.barandgo.crm.recipes.RecipeDTOs.RecipeDTO;
import com.barandgo.crm.recipes.RecipeDTOs.RecipeRequest;

/**
 * Escandallos. Sin borrado: un escandallo en desuso se desactiva.
 */
public interface RecipeService {

    /** Filtro opcional por tipo. Al personal se le ocultan costes y precios. */
    List<RecipeDTO> getRecipes(RecipeType type, CurrentUser requester);

    RecipeDTO getRecipe(Long id, CurrentUser requester);

    RecipeDTO createRecipe(RecipeRequest request, CurrentUser requester);

    RecipeDTO updateRecipe(Long id, RecipeRequest request, CurrentUser requester);

    List<CostHistoryEntry> getCostHistory(Long id);

    /** Recalcula el coste de todos los escandallos y guarda los que han cambiado. Devuelve cuántos. */
    int recalculateAll();

    /** Produce una elaboración inventariable: consume sus ingredientes y da de alta el resultado. */
    ProductionDTO produce(Long id, ProductionRequest request, CurrentUser requester);
}
