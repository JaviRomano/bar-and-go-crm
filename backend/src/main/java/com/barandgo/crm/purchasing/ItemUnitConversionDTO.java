package com.barandgo.crm.purchasing;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Conversión propia de un artículo: 1 fromUnit = factor * toUnit (p. ej. 1 caja = 24 ud).
 */
public record ItemUnitConversionDTO(
        @NotBlank(message = "La unidad de origen es obligatoria")
        String fromUnit,

        @NotBlank(message = "La unidad de destino es obligatoria")
        String toUnit,

        @NotNull(message = "El factor es obligatorio")
        @DecimalMin(value = "0", inclusive = false, message = "El factor debe ser mayor que 0")
        @Digits(integer = 10, fraction = 9, message = "El factor admite hasta 10 enteros y 9 decimales")
        BigDecimal factor) {
}
