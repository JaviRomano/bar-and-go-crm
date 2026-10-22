package com.barandgo.crm.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Recuento de inventario físico.
 */
public final class StockCountDTOs {

    private StockCountDTOs() {
    }

    public enum StockCountStatus {
        OPEN,
        IN_PROGRESS,
        CLOSED
    }

    public record OpenCountRequest(
            @NotNull(message = "El almacén es obligatorio")
            Long warehouseId,

            @Size(max = 500, message = "Las notas no pueden exceder 500 caracteres")
            String notes) {
    }

    /** Cantidades contadas; unit opcional (por defecto, la de inventario). */
    public record CountEntriesRequest(
            @NotEmpty(message = "Indica al menos una cantidad contada")
            List<@Valid CountEntry> entries) {
    }

    public record CountEntry(
            @NotNull(message = "El artículo es obligatorio")
            Long stockItemId,

            @NotNull(message = "La cantidad contada es obligatoria")
            @DecimalMin(value = "0", message = "La cantidad contada no puede ser negativa")
            @Digits(integer = 14, fraction = 4, message = "La cantidad admite hasta 4 decimales")
            BigDecimal countedQuantity,

            String unit) {
    }

    /**
     * Teórico, diferencia y costes se fijan al cerrar y solo se muestran a encargado y administración:
     * quien cuenta no debe ver el teórico (contaría lo que espera encontrar).
     */
    public record StockCountDTO(
            Long id,
            Long warehouseId,
            String warehouseName,
            StockCountStatus status,
            String notes,
            Long openedBy,
            Instant openedAt,
            Long closedBy,
            Instant closedAt,
            List<Line> lines,
            BigDecimal totalDifferenceValue) {
    }

    public record Line(
            Long id,
            Long stockItemId,
            String stockItemCode,
            String stockItemName,
            String unit,
            BigDecimal countedQuantity,
            BigDecimal theoreticalQuantity,
            BigDecimal difference,
            BigDecimal unitCost,
            BigDecimal differenceValue) {
    }
}
