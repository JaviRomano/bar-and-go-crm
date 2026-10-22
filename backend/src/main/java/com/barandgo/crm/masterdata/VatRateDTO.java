package com.barandgo.crm.masterdata;

import java.math.BigDecimal;

import com.barandgo.crm.masterdata.internal.VatRate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Tipo de IVA. En peticiones se ignora el id; active es opcional (por defecto true).
 */
public record VatRateDTO(
        Long id,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 50, message = "El nombre no puede exceder 50 caracteres")
        String name,

        @NotNull(message = "El porcentaje es obligatorio")
        @DecimalMin(value = "0", message = "El porcentaje no puede ser negativo")
        @DecimalMax(value = "99.99", message = "El porcentaje debe ser menor que 100")
        @Digits(integer = 2, fraction = 2, message = "El porcentaje admite hasta 2 decimales")
        BigDecimal rate,

        Boolean active) {

    public static VatRateDTO from(VatRate vatRate) {
        return new VatRateDTO(vatRate.getId(), vatRate.getName(), vatRate.getRate(), vatRate.getActive());
    }
}
