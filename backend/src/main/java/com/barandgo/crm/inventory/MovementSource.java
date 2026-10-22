package com.barandgo.crm.inventory;

/**
 * Documento que origina un movimiento de stock.
 */
public enum MovementSource {
    GOODS_RECEIPT,
    MANUAL,
    TRANSFER,
    STOCK_COUNT,
    ORDER,
    WASTE,
    PRODUCTION
}
