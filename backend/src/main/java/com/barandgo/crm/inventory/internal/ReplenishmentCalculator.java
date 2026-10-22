package com.barandgo.crm.inventory.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Cuánto pedir de un artículo. Se compara con el mínimo lo que hay más lo ya pedido y pendiente de
 * recibir; si no llega, se pide hasta el máximo (o hasta el mínimo si no hay máximo). La cantidad se
 * redondea hacia arriba a unidades de compra enteras: no se piden 2,08 cajas.
 */
final class ReplenishmentCalculator {

    private ReplenishmentCalculator() {
    }

    /** Cantidad a pedir en unidad de inventario, o vacío si no hace falta. */
    static Optional<BigDecimal> suggestedStockQuantity(BigDecimal minStock, BigDecimal maxStock,
                                                       BigDecimal onHand, BigDecimal onOrder) {
        if (minStock == null) {
            return Optional.empty();
        }
        BigDecimal available = onHand.add(onOrder);
        if (available.compareTo(minStock) >= 0) {
            return Optional.empty();
        }
        BigDecimal target = maxStock != null ? maxStock : minStock;
        return Optional.of(target.subtract(available));
    }

    /**
     * @param stockQuantity         cantidad en unidad de inventario
     * @param stockUnitsPerPurchase unidades de inventario que trae una unidad de compra (24 ud por caja)
     */
    static BigDecimal wholePurchaseUnits(BigDecimal stockQuantity, BigDecimal stockUnitsPerPurchase) {
        return stockQuantity.divide(stockUnitsPerPurchase, 0, RoundingMode.CEILING);
    }
}
