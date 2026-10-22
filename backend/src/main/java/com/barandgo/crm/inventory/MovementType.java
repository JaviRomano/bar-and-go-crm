package com.barandgo.crm.inventory;

/**
 * Tipo de movimiento de stock. Correspondencia con el encargo: ENTRADA_COMPRA, SALIDA_VENTA, MERMA,
 * AJUSTE_INVENTARIO, TRASPASO, PRODUCCION_CONSUMO y PRODUCCION_ALTA.
 */
public enum MovementType {
    PURCHASE_RECEIPT,
    SALE,
    WASTE,
    ADJUSTMENT,
    TRANSFER,
    PRODUCTION_CONSUMPTION,
    PRODUCTION_OUTPUT;

    /**
     * Ventas y consumos de producción pueden dejar el stock en negativo (D11): un plato servido no se
     * puede rechazar porque el teórico diga cero, el que está mal es el teórico. Las operaciones
     * manuales (mermas, ajustes, traspasos) no.
     */
    public boolean allowsNegativeStock() {
        return this == SALE || this == PRODUCTION_CONSUMPTION;
    }
}
