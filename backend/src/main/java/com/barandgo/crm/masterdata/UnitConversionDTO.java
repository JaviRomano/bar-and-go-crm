package com.barandgo.crm.masterdata;

import java.math.BigDecimal;

import com.barandgo.crm.masterdata.internal.UnitConversion;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 1 fromUnit = factor * toUnit. Las unidades se indican por código (kg, ml...).
 */
public record UnitConversionDTO(
        Long id,

        @NotBlank(message = "La unidad de origen es obligatoria")
        String fromUnit,

        @NotBlank(message = "La unidad de destino es obligatoria")
        String toUnit,

        @NotNull(message = "El factor es obligatorio")
        @DecimalMin(value = "0", inclusive = false, message = "El factor debe ser mayor que 0")
        @Digits(integer = 10, fraction = 9, message = "El factor admite hasta 10 enteros y 9 decimales")
        BigDecimal factor) {

    public static UnitConversionDTO from(UnitConversion conversion) {
        return new UnitConversionDTO(conversion.getId(), conversion.getFromUnit().getCode(),
                conversion.getToUnit().getCode(), conversion.getFactor());
    }
}
