package com.barandgo.crm.purchasing.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Precio pagado en una compra concreta. Es un registro histórico: no se modifica.
 */
@Entity
@Immutable
@Table(name = "purchase_price_history")
public class PurchasePriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_item_id", nullable = false)
    private Long stockItemId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "unit_id", nullable = false)
    private Long unitId;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "purchased_on", nullable = false)
    private LocalDate purchasedOn;

    // Documento de origen, p. ej. el número del albarán
    @Column(length = 50)
    private String reference;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @PrePersist
    protected void onCreate() {
        this.recordedAt = Instant.now();
    }

    protected PurchasePriceHistory() {
    }

    public PurchasePriceHistory(Long stockItemId, Long supplierId, Long unitId, BigDecimal unitPrice,
                                LocalDate purchasedOn, String reference) {
        this.stockItemId = stockItemId;
        this.supplierId = supplierId;
        this.unitId = unitId;
        this.unitPrice = unitPrice;
        this.purchasedOn = purchasedOn;
        this.reference = reference;
    }

    public Long getId() {
        return id;
    }

    public Long getStockItemId() {
        return stockItemId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public Long getUnitId() {
        return unitId;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public LocalDate getPurchasedOn() {
        return purchasedOn;
    }

    public String getReference() {
        return reference;
    }
}
