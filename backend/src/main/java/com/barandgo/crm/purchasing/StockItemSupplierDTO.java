package com.barandgo.crm.purchasing;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Precio de un artículo en un proveedor, sin IVA, en la unidad en la que lo vende ese proveedor.
 * supplierId, supplierName y updatedAt solo se rellenan en respuestas.
 */
public record StockItemSupplierDTO(
        Long supplierId,

        String supplierName,

        @Size(max = 50, message = "La referencia del proveedor no puede exceder 50 caracteres")
        String supplierItemCode,

        @NotBlank(message = "La unidad de compra es obligatoria")
        String purchaseUnit,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0", message = "El precio no puede ser negativo")
        @Digits(integer = 14, fraction = 4, message = "El precio admite hasta 4 decimales")
        BigDecimal unitPrice,

        Instant updatedAt) {
}
