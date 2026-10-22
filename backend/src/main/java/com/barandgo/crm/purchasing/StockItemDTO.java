package com.barandgo.crm.purchasing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.barandgo.crm.masterdata.Allergen;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Artículo de compra. Las unidades se indican por código (kg, ud, caja...) y el stock mínimo
 * y máximo en la unidad de inventario.
 *
 * <p>En peticiones se ignoran id e itemCategory (nombre, solo en respuestas); perishable,
 * allergens, conversions y active son opcionales.
 */
public record StockItemDTO(
        Long id,

        @NotBlank(message = "El código es obligatorio")
        @Size(max = 30, message = "El código no puede exceder 30 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "El código solo admite letras, números, punto y guiones")
        String code,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
        String name,

        @NotNull(message = "La categoría es obligatoria")
        Long itemCategoryId,

        String itemCategory,

        @NotBlank(message = "La unidad de compra es obligatoria")
        String purchaseUnit,

        @NotBlank(message = "La unidad de inventario es obligatoria")
        String stockUnit,

        @NotBlank(message = "La unidad de uso es obligatoria")
        String usageUnit,

        List<@Valid ItemUnitConversionDTO> conversions,

        Long preferredSupplierId,

        @NotNull(message = "El tipo de IVA es obligatorio")
        Long vatRateId,

        @DecimalMin(value = "0", message = "El stock mínimo no puede ser negativo")
        @Digits(integer = 14, fraction = 4, message = "El stock mínimo admite hasta 4 decimales")
        BigDecimal minStock,

        @DecimalMin(value = "0", message = "El stock máximo no puede ser negativo")
        @Digits(integer = 14, fraction = 4, message = "El stock máximo admite hasta 4 decimales")
        BigDecimal maxStock,

        Boolean perishable,

        Set<Allergen> allergens,

        Boolean active) {
}
