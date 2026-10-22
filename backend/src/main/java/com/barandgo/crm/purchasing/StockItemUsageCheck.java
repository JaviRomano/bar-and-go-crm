package com.barandgo.crm.purchasing;

/**
 * Pregunta de compras al módulo que lleva el stock: ¿tiene movimientos este artículo? Si los tiene,
 * su unidad de inventario ya no puede cambiar (todos los movimientos están en esa unidad). La
 * implementa inventario, de modo que la dependencia va de inventario a compras y no al revés.
 */
public interface StockItemUsageCheck {

    boolean hasStockMovements(Long stockItemId);
}
