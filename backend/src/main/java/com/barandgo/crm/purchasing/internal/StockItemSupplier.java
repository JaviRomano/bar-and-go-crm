package com.barandgo.crm.purchasing.internal;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Un proveedor de un artículo y su precio actual, sin IVA, en la unidad en la que vende.
 */
@Entity
@Table(name = "stock_item_supplier")
public class StockItemSupplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_item_id", nullable = false)
    private StockItem stockItem;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    // Referencia del artículo en el catálogo del proveedor
    @Column(name = "supplier_item_code", length = 50)
    private String supplierItemCode;

    @Column(name = "purchase_unit_id", nullable = false)
    private Long purchaseUnitId;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @PrePersist
    @PreUpdate
    protected void touch() {
        this.updatedAt = Instant.now();
    }

    protected StockItemSupplier() {
    }

    public StockItemSupplier(StockItem stockItem, Long supplierId) {
        this.stockItem = stockItem;
        this.supplierId = supplierId;
    }

    public Long getId() {
        return id;
    }

    public StockItem getStockItem() {
        return stockItem;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public String getSupplierItemCode() {
        return supplierItemCode;
    }

    public void setSupplierItemCode(String supplierItemCode) {
        this.supplierItemCode = supplierItemCode;
    }

    public Long getPurchaseUnitId() {
        return purchaseUnitId;
    }

    public void setPurchaseUnitId(Long purchaseUnitId) {
        this.purchaseUnitId = purchaseUnitId;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
