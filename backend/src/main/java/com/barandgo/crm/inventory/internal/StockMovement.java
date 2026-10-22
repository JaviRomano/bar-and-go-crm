package com.barandgo.crm.inventory.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Immutable;

import com.barandgo.crm.inventory.MovementSource;
import com.barandgo.crm.inventory.MovementType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Movimiento de stock. Inmutable: sin setters, {@code @Immutable} para Hibernate y un trigger en
 * base de datos que rechaza UPDATE y DELETE. Solo lo crea {@link StockLedger}.
 */
@Entity
@Immutable
@Table(name = "stock_movement")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Agrupa los movimientos de una misma operación (un albarán, un traspaso...)
    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MovementType type;

    @Column(name = "stock_item_id", nullable = false)
    private Long stockItemId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "lot_id")
    private Long lotId;

    // Con signo, en unidad de inventario
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 6)
    private BigDecimal unitCost;

    @Column(name = "total_cost", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private MovementSource sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "source_line_id")
    private Long sourceLineId;

    @Column(name = "reverses_movement_id")
    private Long reversesMovementId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 255)
    private String notes;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected StockMovement() {
    }

    StockMovement(UUID operationId, MovementType type, Long stockItemId, Long warehouseId, Long lotId,
                  BigDecimal quantity, BigDecimal unitCost, BigDecimal totalCost, MovementSource sourceType,
                  Long sourceId, Long sourceLineId, Long userId, String notes, Instant occurredAt) {
        this.operationId = operationId;
        this.type = type;
        this.stockItemId = stockItemId;
        this.warehouseId = warehouseId;
        this.lotId = lotId;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.totalCost = totalCost;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.sourceLineId = sourceLineId;
        this.userId = userId;
        this.notes = notes;
        this.occurredAt = occurredAt;
        this.recordedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getOperationId() {
        return operationId;
    }

    public MovementType getType() {
        return type;
    }

    public Long getStockItemId() {
        return stockItemId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public Long getLotId() {
        return lotId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public MovementSource getSourceType() {
        return sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public Long getSourceLineId() {
        return sourceLineId;
    }

    public Long getReversesMovementId() {
        return reversesMovementId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
