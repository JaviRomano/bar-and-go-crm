package com.barandgo.crm.catalog;

import com.barandgo.crm.catalog.internal.ProductCategory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Categoría de carta. En peticiones se ignora el id; sortOrder y active son opcionales.
 */
public record ProductCategoryDTO(
        Long id,

        @NotBlank(message = "El nombre de la categoría es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String name,

        @Min(value = 0, message = "El orden no puede ser negativo")
        Integer sortOrder,

        Boolean active) {

    public static ProductCategoryDTO from(ProductCategory category) {
        return new ProductCategoryDTO(category.getId(), category.getName(),
                category.getSortOrder(), category.getActive());
    }
}
