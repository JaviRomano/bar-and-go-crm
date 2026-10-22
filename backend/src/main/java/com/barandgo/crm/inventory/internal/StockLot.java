package com.barandgo.crm.inventory.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Lote de un artículo perecedero en un almacén. Se crea al recibir (o al traspasar) y se va
 * consumiendo con las salidas, por orden FEFO.
 */
@Entity
@Table(name = "stock_lot")
public class StockLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_item_id", nullable = false)
    private Long stockItemId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "lot_code", nullable = false, length = 50)
    private String lotCode;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "initial_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal initialQuantity;

    @Column(name = "remaining_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal remainingQuantity;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Version
    private Long version;

    protected StockLot() {
    }

    public StockLot(Long stockItemId, Long warehouseId, String lotCode, LocalDate expiryDate,
                    BigDecimal quantity, Instant receivedAt) {
        this.stockItemId = stockItemId;
        this.warehouseId = warehouseId;
        this.lotCode = lotCode;
        this.expiryDate = expiryDate;
        this.initialQuantity = quantity;
        this.remainingQuantity = quantity;
        this.receivedAt = receivedAt;
    }

    public void consume(BigDecimal quantity) {
        this.remainingQuantity = remainingQuantity.subtract(quantity);
    }

    LotAllocator.Lot toAllocatable() {
        return new LotAllocator.Lot(id, expiryDate, receivedAt, remainingQuantity);
    }

    public Long getId() {
        return id;
    }

    public Long getStockItemId() {
        return stockItemId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public String getLotCode() {
        return lotCode;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public BigDecimal getInitialQuantity() {
        return initialQuantity;
    }

    public BigDecimal getRemainingQuantity() {
        return remainingQuantity;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
