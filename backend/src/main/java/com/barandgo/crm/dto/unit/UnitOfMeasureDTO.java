package com.barandgo.crm.dto.unit;

import com.barandgo.crm.entity.UnitOfMeasure;
import com.barandgo.crm.enums.UnitDimension;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UnitOfMeasureDTO(
        Long id,

        @NotBlank(message = "El código es obligatorio")
        @Pattern(regexp = "^[a-z]{1,10}$", message = "El código debe tener de 1 a 10 letras minúsculas")
        String code,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 50, message = "El nombre no puede exceder 50 caracteres")
        String name,

        @NotNull(message = "La magnitud es obligatoria")
        UnitDimension dimension) {

    public static UnitOfMeasureDTO from(UnitOfMeasure unit) {
        return new UnitOfMeasureDTO(unit.getId(), unit.getCode(), unit.getName(), unit.getDimension());
    }
}
