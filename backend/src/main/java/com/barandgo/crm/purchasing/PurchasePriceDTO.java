package com.barandgo.crm.purchasing;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Precio pagado en una compra: proveedor, unidad, precio sin IVA, fecha y documento de origen.
 */
public record PurchasePriceDTO(
        Long supplierId,
        String supplierName,
        String unit,
        BigDecimal unitPrice,
        LocalDate purchasedOn,
        String reference) {
}
