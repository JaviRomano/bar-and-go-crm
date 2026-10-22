package com.barandgo.crm.costcontrol;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Mermas, descuento de stock por pedido y consumo teórico frente a real.
 */
public final class CostControlDTOs {

    private CostControlDTOs() {
    }

    /**
     * Merma de un artículo (quantity en unit; por defecto, la de inventario) o de un plato (quantity en
     * raciones; se descompone con su escandallo). Exactamente uno de stockItemId o productId.
     */
    public record WasteRequest(
            @NotNull(message = "El almacén es obligatorio")
            Long warehouseId,

            Long stockItemId,

            Long productId,

            @NotNull(message = "La cantidad es obligatoria")
            @DecimalMin(value = "0", inclusive = false, message = "La cantidad debe ser mayor que 0")
            @Digits(integer = 14, fraction = 4, message = "La cantidad admite hasta 4 decimales")
            BigDecimal quantity,

            String unit,

            @NotNull(message = "El motivo es obligatorio")
            WasteReason reason,

            @Size(max = 255, message = "Las observaciones no pueden exceder 255 caracteres")
            String notes) {
    }

    public record WasteDTO(
            Long id,
            Long stockItemId,
            String stockItemCode,
            String stockItemName,
            Long productId,
            String productName,
            BigDecimal quantity,
            String unit,
            WasteReason reason,
            Long warehouseId,
            BigDecimal valuedCost,
            String notes,
            Long recordedBy,
            Instant occurredAt) {
    }

    /** Descuento de stock de un pedido entregado (y su anulación, si se canceló después). */
    public record OrderStockPostingDTO(
            Long orderId,
            Long warehouseId,
            Instant postedAt,
            BigDecimal totalCost,
            Integer productsWithoutRecipe,
            Instant reversedAt) {
    }

    /**
     * Consumo de cada artículo entre dos recuentos cerrados del mismo almacén (D15). Cantidades en unidad
     * de inventario; valores al PMP actual.
     */
    public record ConsumptionReportDTO(
            Long warehouseId,
            Long fromCountId,
            Long toCountId,
            Instant from,
            Instant to,
            List<ConsumptionLine> lines,
            BigDecimal totalTheoreticalValue,
            BigDecimal totalRealValue,
            BigDecimal totalDeviationValue,
            BigDecimal totalUnexplainedValue) {
    }

    /**
     * real = inicial + entradas - traspasos de salida - final. teórico = ventas y producción según
     * escandallo. desviación = real - teórico; la parte que no explican las mermas registradas es
     * "unexplained" (robo, raciones mal servidas, escandallo desajustado, mermas sin registrar).
     */
    public record ConsumptionLine(
            Long stockItemId,
            String stockItemCode,
            String stockItemName,
            String unit,
            BigDecimal opening,
            BigDecimal inflows,
            BigDecimal transfersOut,
            BigDecimal closing,
            BigDecimal realConsumption,
            BigDecimal theoreticalConsumption,
            BigDecimal recordedWaste,
            BigDecimal deviation,
            BigDecimal deviationPct,
            BigDecimal unexplained,
            BigDecimal unitCost,
            BigDecimal deviationValue,
            BigDecimal unexplainedValue) {
    }
}
