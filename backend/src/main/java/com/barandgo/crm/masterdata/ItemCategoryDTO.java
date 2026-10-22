package com.barandgo.crm.masterdata;

import com.barandgo.crm.masterdata.internal.ItemCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Categoría de artículo de compra. En peticiones se ignora el id; active es opcional.
 */
public record ItemCategoryDTO(
        Long id,

        @NotBlank(message = "El nombre de la categoría es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String name,

        Boolean active) {

    public static ItemCategoryDTO from(ItemCategory category) {
        return new ItemCategoryDTO(category.getId(), category.getName(), category.getActive());
    }
}
