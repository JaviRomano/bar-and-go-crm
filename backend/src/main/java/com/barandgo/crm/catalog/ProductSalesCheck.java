package com.barandgo.crm.catalog;

/**
 * Pregunta que la carta hace al módulo que registra las ventas: ¿se ha vendido este producto?
 * La carta la usa para archivar en vez de borrar. La implementa el módulo de pedidos, de modo que
 * la dependencia va de pedidos a carta y no al revés.
 */
public interface ProductSalesCheck {

    boolean hasSales(Long productId);
}
