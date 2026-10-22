package com.barandgo.crm.inventory.internal;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Artículo de un recuento. Cantidades en unidad de inventario; teórico, diferencia y coste se fijan
 * al cerrar el recuento.
 */
@Entity
@Table(name = "stock_count_line")
public class StockCountLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_count_id", nullable = false)
    private StockCount stockCount;

    @Column(name = "stock_item_id", nullable = false)
    private Long stockItemId;

    @Column(name = "counted_quantity", precision = 18, scale = 4)
    private BigDecimal countedQuantity;

    @Column(name = "theoretical_quantity", precision = 18, scale = 4)
    private BigDecimal theoreticalQuantity;

    @Column(precision = 18, scale = 4)
    private BigDecimal difference;

    @Column(name = "unit_cost", precision = 19, scale = 6)
    private BigDecimal unitCost;

    @Column(name = "counted_by")
    private Long countedBy;

    @Column(name = "counted_at")
    private Instant countedAt;

    protected StockCountLine() {
    }

    StockCountLine(StockCount stockCount, Long stockItemId) {
        this.stockCount = stockCount;
        this.stockItemId = stockItemId;
    }

    void recordCount(BigDecimal quantity, Long userId) {
        this.countedQuantity = quantity;
        this.countedBy = userId;
        this.countedAt = Instant.now();
    }

    void settle(BigDecimal theoretical, BigDecimal unitCost) {
        this.theoreticalQuantity = theoretical;
        this.difference = countedQuantity.subtract(theoretical);
        this.unitCost = unitCost;
    }

    public boolean isCounted() {
        return countedQuantity != null;
    }

    public Long getId() {
        return id;
    }

    public Long getStockItemId() {
        return stockItemId;
    }

    public BigDecimal getCountedQuantity() {
        return countedQuantity;
    }

    public BigDecimal getTheoreticalQuantity() {
        return theoreticalQuantity;
    }

    public BigDecimal getDifference() {
        return difference;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }
}
