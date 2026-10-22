package com.barandgo.crm.inventory.internal;

import java.math.BigDecimal;

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
 * Línea de albarán: lo recibido en la unidad del proveedor y lo mismo en unidad de inventario, con
 * su coste unitario, que es lo que entra en stock.
 */
@Entity
@Table(name = "goods_receipt_line")
public class GoodsReceiptLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    private GoodsReceipt goodsReceipt;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "purchase_order_line_id")
    private Long purchaseOrderLineId;

    @Column(name = "stock_item_id", nullable = false)
    private Long stockItemId;

    @Column(name = "unit_id", nullable = false)
    private Long unitId;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "stock_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal stockQuantity;

    @Column(name = "stock_unit_cost", nullable = false, precision = 19, scale = 6)
    private BigDecimal stockUnitCost;

    @Column(name = "lot_id")
    private Long lotId;

    protected GoodsReceiptLine() {
    }

    public GoodsReceiptLine(Integer lineNumber, Long purchaseOrderLineId, Long stockItemId, Long unitId,
                            BigDecimal quantity, BigDecimal unitPrice, BigDecimal stockQuantity,
                            BigDecimal stockUnitCost) {
        this.lineNumber = lineNumber;
        this.purchaseOrderLineId = purchaseOrderLineId;
        this.stockItemId = stockItemId;
        this.unitId = unitId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.stockQuantity = stockQuantity;
        this.stockUnitCost = stockUnitCost;
    }

    void setGoodsReceipt(GoodsReceipt goodsReceipt) {
        this.goodsReceipt = goodsReceipt;
    }

    void setLotId(Long lotId) {
        this.lotId = lotId;
    }

    public Long getId() {
        return id;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public Long getPurchaseOrderLineId() {
        return purchaseOrderLineId;
    }

    public Long getStockItemId() {
        return stockItemId;
    }

    public Long getUnitId() {
        return unitId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getStockQuantity() {
        return stockQuantity;
    }

    public BigDecimal getStockUnitCost() {
        return stockUnitCost;
    }

    public Long getLotId() {
        return lotId;
    }
}
