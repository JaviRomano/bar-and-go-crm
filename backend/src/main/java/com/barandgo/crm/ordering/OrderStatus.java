package com.barandgo.crm.ordering;

/**
 * Estado de un pedido para recoger (D9).
 *
 * <pre>
 * CREATED -> IN_PREPARATION -> READY -> DELIVERED
 *    \______________\___________\________\_______-> CANCELLED
 * </pre>
 *
 * Cancelar un pedido entregado (devolución) solo lo hace encargado o administración, y deshace el
 * descuento de stock.
 */
public enum OrderStatus {
    CREATED,
    IN_PREPARATION,
    READY,
    DELIVERED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case CREATED -> target == IN_PREPARATION || target == CANCELLED;
            case IN_PREPARATION -> target == READY || target == CANCELLED;
            case READY -> target == DELIVERED || target == CANCELLED;
            case DELIVERED -> target == CANCELLED;
            case CANCELLED -> false;
        };
    }
}
