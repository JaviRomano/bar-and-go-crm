package com.barandgo.crm.ordering.internal;

import org.springframework.stereotype.Component;

import com.barandgo.crm.catalog.ProductSalesCheck;

/**
 * Un producto tiene ventas si aparece en alguna línea de pedido, en cualquier estado.
 */
@Component
class OrderedProductSalesCheck implements ProductSalesCheck {

    private final OrderItemRepository orderItemRepository;

    OrderedProductSalesCheck(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public boolean hasSales(Long productId) {
        return orderItemRepository.existsByProductId(productId);
    }
}
