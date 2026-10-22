package com.barandgo.crm.recipes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.barandgo.crm.inventory.InventoryDTOs.StockMovementDTO;
import com.barandgo.crm.masterdata.Allergen;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Escandallos: petición, respuesta, histórico de coste y producción.
 */
public final class RecipeDTOs {

    private RecipeDTOs() {
    }

    /**
     * DISH: productId y portions obligatorios. PREPARATION: yieldQuantity y yieldUnit obligatorios;
     * stockItemId si la elaboración se produce y se guarda en stock.
     */
    public record RecipeRequest(
            @NotBlank(message = "El nombre es obligatorio")
            @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
            String name,

            @NotNull(message = "El tipo es obligatorio")
            RecipeType type,

            Long productId,

            Long stockItemId,

            @DecimalMin(value = "0", inclusive = false, message = "Las raciones deben ser mayores que 0")
            @Digits(integer = 8, fraction = 2, message = "Las raciones admiten hasta 2 decimales")
            BigDecimal portions,

            @DecimalMin(value = "0", inclusive = false, message = "El rendimiento debe ser mayor que 0")
            @Digits(integer = 14, fraction = 4, message = "El rendimiento admite hasta 4 decimales")
            BigDecimal yieldQuantity,

            String yieldUnit,

            @DecimalMin(value = "0", inclusive = false, message = "El food cost objetivo debe ser mayor que 0")
            @DecimalMax(value = "99.99", message = "El food cost objetivo debe ser menor que 100")
            BigDecimal targetFoodCostPct,

            @Size(max = 500, message = "Las notas no pueden exceder 500 caracteres")
            String notes,

            Boolean active,

            @NotEmpty(message = "El escandallo debe tener al menos un ingrediente")
            List<@Valid LineRequest> lines) {
    }

    /** Ingrediente: exactamente uno de stockItemId (artículo) o subRecipeId (elaboración). */
    public record LineRequest(
            Long stockItemId,

            Long subRecipeId,

            @NotNull(message = "La cantidad bruta es obligatoria")
            @DecimalMin(value = "0", inclusive = false, message = "La cantidad debe ser mayor que 0")
            @Digits(integer = 14, fraction = 4, message = "La cantidad admite hasta 4 decimales")
            BigDecimal grossQuantity,

            @NotBlank(message = "La unidad es obligatoria")
            String unit,

            @DecimalMin(value = "0", inclusive = false, message = "El rendimiento debe ser mayor que 0")
            @DecimalMax(value = "100", message = "El rendimiento no puede superar el 100 %")
            BigDecimal yieldPct) {
    }

    /**
     * Escandallo con su coste. Los campos de coste y precio van a null para el personal (lectura sin
     * costes) y cuando el coste está incompleto en los que dependen de él.
     */
    public record RecipeDTO(
            Long id,
            String name,
            RecipeType type,
            Long productId,
            String productName,
            BigDecimal productPrice,
            BigDecimal vatRate,
            Long stockItemId,
            String stockItemCode,
            BigDecimal portions,
            BigDecimal yieldQuantity,
            String yieldUnit,
            BigDecimal targetFoodCostPct,
            String notes,
            Boolean active,
            List<Line> lines,
            Set<Allergen> allergens,
            BigDecimal totalCost,
            BigDecimal costPerPortion,
            BigDecimal costPerYieldUnit,
            Boolean costComplete,
            List<String> missingCostItems,
            BigDecimal foodCostPct,
            BigDecimal grossMargin,
            BigDecimal suggestedPrice) {
    }

    public record Line(
            Integer lineNumber,
            Long stockItemId,
            String stockItemCode,
            String stockItemName,
            Long subRecipeId,
            String subRecipeName,
            BigDecimal grossQuantity,
            String unit,
            BigDecimal yieldPct,
            BigDecimal netQuantity,
            BigDecimal unitCost,
            BigDecimal lineCost) {
    }

    public record CostHistoryEntry(BigDecimal totalCost, BigDecimal costPerUnit, Boolean complete, String reason,
                                   Instant recordedAt) {
    }

    /** Cantidad a producir, en la unidad de rendimiento salvo que se indique otra. */
    public record ProductionRequest(
            @NotNull(message = "El almacén es obligatorio")
            Long warehouseId,

            @NotNull(message = "La cantidad es obligatoria")
            @DecimalMin(value = "0", inclusive = false, message = "La cantidad debe ser mayor que 0")
            @Digits(integer = 14, fraction = 4, message = "La cantidad admite hasta 4 decimales")
            BigDecimal quantity,

            String unit,

            @Size(max = 255, message = "Las notas no pueden exceder 255 caracteres")
            String notes) {
    }

    /** Producción registrada: consumos y alta, con el coste real (PMP de lo consumido). */
    public record ProductionDTO(
            Long id,
            Long recipeId,
            String recipeName,
            Long warehouseId,
            BigDecimal quantity,
            String unit,
            BigDecimal totalCost,
            BigDecimal unitCost,
            UUID operationId,
            List<StockMovementDTO> movements) {
    }
}
