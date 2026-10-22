package com.barandgo.crm.costcontrol.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Immutable;

import com.barandgo.crm.costcontrol.WasteReason;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Merma registrada. Los movimientos WASTE que genera comparten {@code operationId}; para corregir una
 * merma mal registrada se hace un ajuste, no se edita.
 */
@Entity
@Immutable
@Table(name = "waste")
public class Waste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_item_id")
    private Long stockItemId;

    @Column(name = "product_id")
    private Long productId;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_id")
    private Long unitId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WasteReason reason;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "valued_cost", nullable = false, precision = 18, scale = 4)
    private BigDecimal valuedCost;

    @Column(length = 255)
    private String notes;

    @Column(name = "recorded_by", nullable = false)
    private Long recordedBy;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    protected Waste() {
    }

    Waste(Long stockItemId, Long productId, BigDecimal quantity, Long unitId, WasteReason reason, Long warehouseId,
          BigDecimal valuedCost, String notes, Long recordedBy, UUID operationId) {
        this.stockItemId = stockItemId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitId = unitId;
        this.reason = reason;
        this.warehouseId = warehouseId;
        this.valuedCost = valuedCost;
        this.notes = notes;
        this.recordedBy = recordedBy;
        this.operationId = operationId;
        this.occurredAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getStockItemId() {
        return stockItemId;
    }

    public Long getProductId() {
        return productId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public Long getUnitId() {
        return unitId;
    }

    public WasteReason getReason() {
        return reason;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public BigDecimal getValuedCost() {
        return valuedCost;
    }

    public String getNotes() {
        return notes;
    }

    public Long getRecordedBy() {
        return recordedBy;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
