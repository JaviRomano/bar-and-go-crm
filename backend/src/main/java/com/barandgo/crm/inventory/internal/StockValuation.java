package com.barandgo.crm.inventory.internal;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Valoración PMP de un artículo en un local (D1). Es por local y no por almacén: un traspaso entre
 * almacenes del mismo local no cambia el precio medio.
 */
@Entity
@Table(name = "stock_valuation")
public class StockValuation {

    @Embeddable
    public static class Key implements Serializable {

        @Column(name = "stock_item_id")
        private Long stockItemId;

        @Column(name = "site_id")
        private Long siteId;

        protected Key() {
        }

        public Key(Long stockItemId, Long siteId) {
            this.stockItemId = stockItemId;
            this.siteId = siteId;
        }

        public Long getStockItemId() {
            return stockItemId;
        }

        public Long getSiteId() {
            return siteId;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key that
                    && Objects.equals(stockItemId, that.stockItemId)
                    && Objects.equals(siteId, that.siteId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(stockItemId, siteId);
        }
    }

    @EmbeddedId
    private Key id;

    @Column(name = "quantity_on_hand", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityOnHand;

    @Column(name = "avg_cost", nullable = false, precision = 19, scale = 6)
    private BigDecimal averageCost;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StockValuation() {
    }

    void receive(BigDecimal quantity, BigDecimal unitCost) {
        this.averageCost = MovingAverage.afterReceipt(quantityOnHand, averageCost, quantity, unitCost);
        this.quantityOnHand = quantityOnHand.add(quantity);
        this.updatedAt = Instant.now();
    }

    void issue(BigDecimal quantity) {
        this.quantityOnHand = quantityOnHand.subtract(quantity);
        this.updatedAt = Instant.now();
    }

    public Key getId() {
        return id;
    }

    public Long getStockItemId() {
        return id.getStockItemId();
    }

    public Long getSiteId() {
        return id.getSiteId();
    }

    public BigDecimal getQuantityOnHand() {
        return quantityOnHand;
    }

    public BigDecimal getAverageCost() {
        return averageCost;
    }
}
