package com.barandgo.crm.recipes.internal;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.recipes.RecipeDTOs.CostHistoryEntry;
import com.barandgo.crm.recipes.RecipeDTOs.ProductionDTO;
import com.barandgo.crm.recipes.RecipeDTOs.ProductionRequest;
import com.barandgo.crm.recipes.RecipeDTOs.RecipeDTO;
import com.barandgo.crm.recipes.RecipeDTOs.RecipeRequest;
import com.barandgo.crm.recipes.RecipeService;
import com.barandgo.crm.recipes.RecipeType;

import jakarta.validation.Valid;

/**
 * Escandallos. Sin DELETE: se desactivan con active=false en el PUT.
 */
@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public ResponseEntity<List<RecipeDTO>> getRecipes(@AuthenticationPrincipal Jwt jwt,
                                                      @RequestParam(required = false) RecipeType type) {
        return ResponseEntity.ok(recipeService.getRecipes(type, CurrentUser.from(jwt)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipeDTO> getRecipe(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ResponseEntity.ok(recipeService.getRecipe(id, CurrentUser.from(jwt)));
    }

    @PostMapping
    public ResponseEntity<RecipeDTO> createRecipe(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody RecipeRequest request) {
        return new ResponseEntity<>(recipeService.createRecipe(request, CurrentUser.from(jwt)), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecipeDTO> updateRecipe(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                                  @Valid @RequestBody RecipeRequest request) {
        return ResponseEntity.ok(recipeService.updateRecipe(id, request, CurrentUser.from(jwt)));
    }

    @GetMapping("/{id}/cost-history")
    public ResponseEntity<List<CostHistoryEntry>> getCostHistory(@PathVariable Long id) {
        return ResponseEntity.ok(recipeService.getCostHistory(id));
    }

    @PostMapping("/recalculate")
    public ResponseEntity<Map<String, Integer>> recalculate() {
        return ResponseEntity.ok(Map.of("changed", recipeService.recalculateAll()));
    }

    @PostMapping("/{id}/production")
    public ResponseEntity<ProductionDTO> produce(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                                 @Valid @RequestBody ProductionRequest request) {
        return new ResponseEntity<>(recipeService.produce(id, request, CurrentUser.from(jwt)), HttpStatus.CREATED);
    }
}
