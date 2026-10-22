package com.barandgo.crm.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Almacén. En peticiones se ignora el id; siteId es opcional (por defecto, el único local), active
 * también (por defecto true) y defaultForSales (por defecto false). Marcar un almacén como origen de las
 * ventas desmarca el que lo fuera antes en su local.
 */
public record WarehouseDTO(
        Long id,

        Long siteId,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String name,

        @NotNull(message = "El tipo de almacén es obligatorio")
        WarehouseType type,

        Boolean active,

        Boolean defaultForSales) {
}
