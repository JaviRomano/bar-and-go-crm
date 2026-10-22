package com.barandgo.crm.purchasing;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta o sustitución de un pedido en borrador. Si no se indica expectedDate, se calcula con el
 * plazo de entrega habitual del proveedor.
 */
public record PurchaseOrderRequestDTO(
        @NotNull(message = "El proveedor es obligatorio")
        Long supplierId,

        LocalDate expectedDate,

        @Size(max = 500, message = "Las notas no pueden exceder 500 caracteres")
        String notes,

        @NotEmpty(message = "El pedido debe tener al menos una línea")
        List<@Valid PurchaseOrderLineRequestDTO> lines) {
}
