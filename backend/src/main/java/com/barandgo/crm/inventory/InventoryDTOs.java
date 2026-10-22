package com.barandgo.crm.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTOs de consulta y operación del inventario. Las cantidades van en la unidad de inventario del
 * artículo salvo que una petición indique otra unidad.
 */
public final class InventoryDTOs {

    private InventoryDTOs() {
    }

    /** Stock de un artículo en un almacén. Sin costes: lo consulta también el personal. */
    public record StockBalanceDTO(
            Long stockItemId,
            String stockItemCode,
            String stockItemName,
            String unit,
            Long warehouseId,
            String warehouseName,
            BigDecimal quantity) {
    }

    /** Valoración PMP de un artículo en un local. */
    public record ValuationLineDTO(
            Long stockItemId,
            String stockItemCode,
            String stockItemName,
            String unit,
            Long siteId,
            BigDecimal quantity,
            BigDecimal averageCost,
            BigDecimal value) {
    }

    public record ValuationReportDTO(List<ValuationLineDTO> lines, BigDecimal totalValue) {
    }

    public record StockMovementDTO(
            Long id,
            UUID operationId,
            MovementType type,
            Long stockItemId,
            String stockItemCode,
            String stockItemName,
            Long warehouseId,
            String warehouseName,
            String lotCode,
            BigDecimal quantity,
            String unit,
            BigDecimal unitCost,
            BigDecimal totalCost,
            MovementSource sourceType,
            Long sourceId,
            Long userId,
            String notes,
            Instant occurredAt) {
    }

    /**
     * Ajuste manual. quantity con signo; unit opcional (por defecto, la de inventario). unitCost solo
     * cuenta en ajustes positivos; si no se indica, se valora al PMP.
     */
    public record AdjustmentRequestDTO(
            @NotNull(message = "El almacén es obligatorio")
            Long warehouseId,

            @NotNull(message = "El artículo es obligatorio")
            Long stockItemId,

            @NotNull(message = "La cantidad es obligatoria")
            @Digits(integer = 14, fraction = 4, message = "La cantidad admite hasta 4 decimales")
            BigDecimal quantity,

            String unit,

            @DecimalMin(value = "0", message = "El coste no puede ser negativo")
            @Digits(integer = 13, fraction = 6, message = "El coste admite hasta 6 decimales")
            BigDecimal unitCost,

            @NotBlank(message = "El motivo es obligatorio")
            @Size(max = 255, message = "El motivo no puede exceder 255 caracteres")
            String reason) {
    }

    public record TransferRequestDTO(
            @NotNull(message = "El almacén de origen es obligatorio")
            Long fromWarehouseId,

            @NotNull(message = "El almacén de destino es obligatorio")
            Long toWarehouseId,

            @NotNull(message = "El artículo es obligatorio")
            Long stockItemId,

            @NotNull(message = "La cantidad es obligatoria")
            @DecimalMin(value = "0", inclusive = false, message = "La cantidad debe ser mayor que 0")
            @Digits(integer = 14, fraction = 4, message = "La cantidad admite hasta 4 decimales")
            BigDecimal quantity,

            String unit,

            @Size(max = 255, message = "Las notas no pueden exceder 255 caracteres")
            String notes) {
    }

    /**
     * Suma de los movimientos de un artículo en un almacén, por tipo. value lleva signo (negativo en las
     * salidas) y, al ser el ledger valorado a PMP, la suma de todos los tipos hasta una fecha es el valor
     * contable del stock en esa fecha.
     */
    public record MovementTotalDTO(
            Long stockItemId,
            Long warehouseId,
            MovementType type,
            BigDecimal quantity,
            BigDecimal value) {
    }

    public record ExpiringLotDTO(
            Long lotId,
            Long stockItemId,
            String stockItemCode,
            String stockItemName,
            Long warehouseId,
            String warehouseName,
            String lotCode,
            LocalDate expiryDate,
            BigDecimal remainingQuantity,
            String unit,
            long daysLeft) {
    }
}
