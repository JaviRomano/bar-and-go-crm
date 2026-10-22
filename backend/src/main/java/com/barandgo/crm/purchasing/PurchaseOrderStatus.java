package com.barandgo.crm.purchasing;

/**
 * Ciclo de vida de un pedido a proveedor.
 *
 * <pre>
 * DRAFT ──> SENT ──> PARTIALLY_RECEIVED ──> RECEIVED
 *   │         │  └──────────────────────────────^
 *   └─────────┴──> CANCELLED
 * </pre>
 *
 * Los estados de recepción los fija la recepción de albaranes (Fase 3). Un pedido con algo
 * recibido ya no se cancela: se da por recibido aunque falte mercancía.
 */
public enum PurchaseOrderStatus {
    DRAFT,
    SENT,
    PARTIALLY_RECEIVED,
    RECEIVED,
    CANCELLED;

    public boolean canTransitionTo(PurchaseOrderStatus target) {
        return switch (this) {
            case DRAFT -> target == SENT || target == CANCELLED;
            case SENT -> target == PARTIALLY_RECEIVED || target == RECEIVED || target == CANCELLED;
            case PARTIALLY_RECEIVED -> target == RECEIVED;
            case RECEIVED, CANCELLED -> false;
        };
    }
}
