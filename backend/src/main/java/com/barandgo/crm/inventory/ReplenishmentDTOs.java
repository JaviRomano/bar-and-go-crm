package com.barandgo.crm.inventory;

import java.math.BigDecimal;
import java.util.List;

/**
 * Sugerencia de reposición agrupada por proveedor. El grupo con supplierId null reúne los artículos
 * bajo mínimo sin proveedor con tarifa: hay que pedirlos a mano.
 */
public final class ReplenishmentDTOs {

    private ReplenishmentDTOs() {
    }

    public record SupplierGroup(Long supplierId, String supplierName, List<Line> lines) {
    }

    public record Line(
            Long stockItemId,
            String stockItemCode,
            String stockItemName,
            String stockUnit,
            BigDecimal onHand,
            BigDecimal onOrder,
            BigDecimal minStock,
            BigDecimal maxStock,
            BigDecimal suggestedStockQuantity,
            String purchaseUnit,
            BigDecimal suggestedPurchaseQuantity,
            BigDecimal unitPrice) {
    }

    /** Proveedores para los que crear pedidos en borrador; vacío o null: todos los que tengan tarifa. */
    public record CreateOrdersRequest(List<Long> supplierIds) {
    }
}
