package com.barandgo.crm.inventory.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Albarán de recepción. Se guarda ya contabilizado y no se modifica.
 */
@Entity
@Table(name = "goods_receipt")
public class GoodsReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // AL-<año>-<secuencia>
    @Column(nullable = false, unique = true, length = 20, updatable = false)
    private String number;

    @Column(name = "supplier_id", nullable = false, updatable = false)
    private Long supplierId;

    // Número del albarán del proveedor
    @Column(name = "supplier_delivery_note", length = 50, updatable = false)
    private String supplierDeliveryNote;

    @Column(name = "purchase_order_id", updatable = false)
    private Long purchaseOrderId;

    @Column(name = "warehouse_id", nullable = false, updatable = false)
    private Long warehouseId;

    @Column(name = "receipt_date", nullable = false, updatable = false)
    private LocalDate receiptDate;

    @Column(length = 500, updatable = false)
    private String notes;

    @Column(name = "received_by", nullable = false, updatable = false)
    private Long receivedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL)
    @OrderBy("lineNumber")
    private List<GoodsReceiptLine> lines = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    protected GoodsReceipt() {
    }

    public GoodsReceipt(String number, Long supplierId, String supplierDeliveryNote, Long purchaseOrderId,
                        Long warehouseId, LocalDate receiptDate, String notes, Long receivedBy) {
        this.number = number;
        this.supplierId = supplierId;
        this.supplierDeliveryNote = supplierDeliveryNote;
        this.purchaseOrderId = purchaseOrderId;
        this.warehouseId = warehouseId;
        this.receiptDate = receiptDate;
        this.notes = notes;
        this.receivedBy = receivedBy;
    }

    public void addLine(GoodsReceiptLine line) {
        lines.add(line);
        line.setGoodsReceipt(this);
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public String getSupplierDeliveryNote() {
        return supplierDeliveryNote;
    }

    public Long getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public LocalDate getReceiptDate() {
        return receiptDate;
    }

    public String getNotes() {
        return notes;
    }

    public Long getReceivedBy() {
        return receivedBy;
    }

    public List<GoodsReceiptLine> getLines() {
        return lines;
    }
}
