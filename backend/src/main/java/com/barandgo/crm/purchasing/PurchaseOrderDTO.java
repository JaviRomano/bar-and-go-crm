package com.barandgo.crm.purchasing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Pedido a proveedor con sus importes. Base e IVA se calculan por tipo de IVA, como en una factura.
 */
public record PurchaseOrderDTO(
        Long id,
        String number,
        Long supplierId,
        String supplierName,
        PurchaseOrderStatus status,
        LocalDate orderDate,
        LocalDate expectedDate,
        String notes,
        Long createdBy,
        Instant sentAt,
        Instant cancelledAt,
        List<Line> lines,
        List<VatBreakdown> vatBreakdown,
        BigDecimal totalBase,
        BigDecimal totalVat,
        BigDecimal total) {

    public record Line(
            Long id,
            Integer lineNumber,
            Long stockItemId,
            String stockItemCode,
            String stockItemName,
            String unit,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal vatRate,
            BigDecimal amount,
            BigDecimal receivedQuantity) {
    }

    public record VatBreakdown(BigDecimal vatRate, BigDecimal base, BigDecimal vat) {
    }
}
