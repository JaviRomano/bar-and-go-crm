package com.barandgo.crm.costcontrol;

/**
 * Motivo de una merma. Correspondencia con el encargo: CADUCIDAD, ROTURA, ELABORACION,
 * DEVOLUCION_CLIENTE, INVITACION, CONSUMO_PERSONAL, ERROR_COMANDA y OTRO.
 */
public enum WasteReason {
    EXPIRED,
    BREAKAGE,
    PREPARATION,
    CUSTOMER_RETURN,
    COMPLIMENTARY,
    STAFF_MEAL,
    ORDER_ERROR,
    OTHER
}
