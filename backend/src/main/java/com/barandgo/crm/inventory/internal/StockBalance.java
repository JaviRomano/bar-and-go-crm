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
 * Saldo de un artículo en un almacén. Lo actualiza {@link StockLedger} en la misma transacción que el
 * movimiento, con la fila bloqueada (SELECT ... FOR UPDATE): no lleva {@code @Version} porque en
 * horas punta los reintentos de un bloqueo optimista serían constantes.
 */
@Entity
@Table(name = "stock_balance")
public class StockBalance {

    @Embeddable
    public static class Key implements Serializable {

        @Column(name = "stock_item_id")
        private Long stockItemId;

        @Column(name = "warehouse_id")
        private Long warehouseId;

        protected Key() {
        }

        public Key(Long stockItemId, Long warehouseId) {
            this.stockItemId = stockItemId;
            this.warehouseId = warehouseId;
        }

        public Long getStockItemId() {
            return stockItemId;
        }

        public Long getWarehouseId() {
            return warehouseId;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key that
                    && Objects.equals(stockItemId, that.stockItemId)
                    && Objects.equals(warehouseId, that.warehouseId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(stockItemId, warehouseId);
        }
    }

    @EmbeddedId
    private Key id;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StockBalance() {
    }

    void add(BigDecimal delta) {
        this.quantity = quantity.add(delta);
        this.updatedAt = Instant.now();
    }

    public Key getId() {
        return id;
    }

    public Long getStockItemId() {
        return id.getStockItemId();
    }

    public Long getWarehouseId() {
        return id.getWarehouseId();
    }

    public BigDecimal getQuantity() {
        return quantity;
    }
}
