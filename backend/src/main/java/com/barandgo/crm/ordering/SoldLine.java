package com.barandgo.crm.ordering;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Línea de un pedido entregado y no cancelado, con el precio cobrado (IVA incluido). productId es null en
 * líneas antiguas sin producto enlazado.
 */
public record SoldLine(Long orderId, Instant deliveredAt, Long productId, String productName, Integer quantity,
                       BigDecimal unitPrice) {
}
