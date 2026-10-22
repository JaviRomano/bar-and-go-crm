package com.barandgo.crm.purchasing;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/**
 * Línea de pedido entrante. unit y unitPrice son opcionales: por defecto se toman del precio del
 * artículo en ese proveedor (convirtiendo el precio si se pide en otra unidad).
 */
public record PurchaseOrderLineRequestDTO(
        @NotNull(message = "El artículo es obligatorio")
        Long stockItemId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0", inclusive = false, message = "La cantidad debe ser mayor que 0")
        @Digits(integer = 14, fraction = 4, message = "La cantidad admite hasta 4 decimales")
        BigDecimal quantity,

        String unit,

        @DecimalMin(value = "0", message = "El precio no puede ser negativo")
        @Digits(integer = 14, fraction = 4, message = "El precio admite hasta 4 decimales")
        BigDecimal unitPrice) {
}
