package com.barandgo.crm.costcontrol.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Descuento de stock de un pedido. La clave es el pedido: un segundo evento de entrega del mismo pedido
 * (reintento del registro de eventos) encuentra la fila y no descuenta otra vez.
 */
@Entity
@Table(name = "order_stock_posting")
public class OrderStockPosting {

    @Id
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "operation_id")
    private UUID operationId;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "total_cost", precision = 18, scale = 4)
    private BigDecimal totalCost;

    @Column(name = "products_without_recipe", nullable = false)
    private Integer productsWithoutRecipe = 0;

    @Column(name = "reversed_at")
    private Instant reversedAt;

    @Column(name = "reversal_operation_id")
    private UUID reversalOperationId;

    protected OrderStockPosting() {
    }

    static OrderStockPosting posted(Long orderId, Long warehouseId, UUID operationId, BigDecimal totalCost,
                                    int productsWithoutRecipe) {
        OrderStockPosting posting = new OrderStockPosting();
        posting.orderId = orderId;
        posting.warehouseId = warehouseId;
        posting.operationId = operationId;
        posting.postedAt = Instant.now();
        posting.totalCost = totalCost;
        posting.productsWithoutRecipe = productsWithoutRecipe;
        return posting;
    }

    /** La cancelación llegó antes que la entrega: no hay nada que anular y la entrega no descontará. */
    static OrderStockPosting cancelledBeforePosting(Long orderId) {
        OrderStockPosting posting = new OrderStockPosting();
        posting.orderId = orderId;
        posting.reversedAt = Instant.now();
        return posting;
    }

    void markReversed(UUID reversalOperationId) {
        this.reversedAt = Instant.now();
        this.reversalOperationId = reversalOperationId;
    }

    boolean isReversed() {
        return reversedAt != null;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public Integer getProductsWithoutRecipe() {
        return productsWithoutRecipe;
    }

    public Instant getReversedAt() {
        return reversedAt;
    }
}
