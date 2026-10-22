package com.barandgo.crm.masterdata;

import java.time.Instant;

import com.barandgo.crm.masterdata.internal.Supplier;
import com.barandgo.crm.masterdata.internal.TaxIdValidator;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Proveedor. En peticiones se ignoran id y createdAt; active es opcional (por defecto true).
 */
public record SupplierDTO(
        Long id,

        @NotBlank(message = "El CIF/NIF es obligatorio")
        @TaxIdValidator
        String taxId,

        @NotBlank(message = "La razón social es obligatoria")
        @Size(max = 200, message = "La razón social no puede exceder 200 caracteres")
        String legalName,

        @Size(max = 200, message = "El nombre comercial no puede exceder 200 caracteres")
        String tradeName,

        @Size(max = 100, message = "El contacto no puede exceder 100 caracteres")
        String contactName,

        @Email(message = "El email debe ser válido")
        @Size(max = 150, message = "El email no puede exceder 150 caracteres")
        String email,

        @Pattern(regexp = "^[+]?[0-9]{9,20}$", message = "El teléfono debe ser válido")
        String phone,

        @Size(max = 200, message = "Las condiciones de pago no pueden exceder 200 caracteres")
        String paymentTerms,

        @Min(value = 0, message = "El plazo de entrega no puede ser negativo")
        Integer leadTimeDays,

        @Size(max = 500, message = "Las notas no pueden exceder 500 caracteres")
        String notes,

        Boolean active,

        Instant createdAt) {

    public static SupplierDTO from(Supplier supplier) {
        return new SupplierDTO(supplier.getId(), supplier.getTaxId(), supplier.getLegalName(),
                supplier.getTradeName(), supplier.getContactName(), supplier.getEmail(), supplier.getPhone(),
                supplier.getPaymentTerms(), supplier.getLeadTimeDays(), supplier.getNotes(),
                supplier.getActive(), supplier.getCreatedAt());
    }
}
