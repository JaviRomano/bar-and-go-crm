package com.barandgo.crm.ordering;

import java.time.Instant;
import java.util.List;

/**
 * Eventos de pedidos para otros módulos. Pedidos no sabe quién los escucha: control de costes los usa
 * para descontar y reponer stock según el escandallo de cada producto.
 */
public final class OrderEvents {

    private OrderEvents() {
    }

    /** Un pedido se ha entregado: lo vendido sale del stock. */
    public record OrderDelivered(Long orderId, Long deliveredBy, Instant deliveredAt, List<Line> lines) {

        /** productId null en líneas antiguas sin enlace con la carta. */
        public record Line(Long productId, Integer quantity) {
        }
    }

    /** Un pedido entregado se ha cancelado (devolución): lo descontado vuelve al stock. */
    public record OrderDeliveryReverted(Long orderId, Long revertedBy) {
    }
}
