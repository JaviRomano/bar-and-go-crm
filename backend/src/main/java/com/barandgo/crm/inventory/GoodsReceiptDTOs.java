package com.barandgo.crm.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Albarán de recepción: petición de alta y respuesta.
 */
public final class GoodsReceiptDTOs {

    private GoodsReceiptDTOs() {
    }

    /**
     * Si se indica purchaseOrderId, cada línea se cruza con la línea del pedido de su artículo, y de
     * ella salen unidad y precio por defecto. receiptDate por defecto: hoy.
     */
    public record GoodsReceiptRequestDTO(
            @NotNull(message = "El proveedor es obligatorio")
            Long supplierId,

            Long purchaseOrderId,

            @NotNull(message = "El almacén es obligatorio")
            Long warehouseId,

            @Size(max = 50, message = "El número de albarán no puede exceder 50 caracteres")
            String supplierDeliveryNote,

            LocalDate receiptDate,

            @Size(max = 500, message = "Las notas no pueden exceder 500 caracteres")
            String notes,

            @NotEmpty(message = "El albarán debe tener al menos una línea")
            List<@Valid LineRequest> lines) {
    }

    /**
     * Línea recibida. unit y unitPrice son opcionales: por defecto, los de la línea del pedido o, sin
     * pedido, los de la tarifa del proveedor. Lote y caducidad son obligatorios en perecederos.
     */
    public record LineRequest(
            @NotNull(message = "El artículo es obligatorio")
            Long stockItemId,

            @NotNull(message = "La cantidad es obligatoria")
            @DecimalMin(value = "0", inclusive = false, message = "La cantidad debe ser mayor que 0")
            @Digits(integer = 14, fraction = 4, message = "La cantidad admite hasta 4 decimales")
            BigDecimal quantity,

            String unit,

            @DecimalMin(value = "0", message = "El precio no puede ser negativo")
            @Digits(integer = 14, fraction = 4, message = "El precio admite hasta 4 decimales")
            BigDecimal unitPrice,

            @Size(max = 50, message = "El lote no puede exceder 50 caracteres")
            String lotCode,

            LocalDate expiryDate) {
    }

    public record GoodsReceiptDTO(
            Long id,
            String number,
            Long supplierId,
            String supplierName,
            String supplierDeliveryNote,
            Long purchaseOrderId,
            Long warehouseId,
            String warehouseName,
            LocalDate receiptDate,
            String notes,
            Long receivedBy,
            List<Line> lines,
            BigDecimal totalBase) {
    }

    public record Line(
            Long id,
            Integer lineNumber,
            Long purchaseOrderLineId,
            Long stockItemId,
            String stockItemCode,
            String stockItemName,
            String unit,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal amount,
            BigDecimal stockQuantity,
            String stockUnit,
            BigDecimal stockUnitCost,
            String lotCode,
            LocalDate expiryDate) {
    }
}
