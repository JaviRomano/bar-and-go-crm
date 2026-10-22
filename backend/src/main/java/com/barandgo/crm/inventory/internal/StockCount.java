package com.barandgo.crm.inventory.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.barandgo.crm.inventory.StockCountDTOs.StockCountStatus;
import com.barandgo.crm.shared.ConflictException;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "stock_count")
public class StockCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "warehouse_id", nullable = false, updatable = false)
    private Long warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockCountStatus status = StockCountStatus.OPEN;

    @Column(length = 500)
    private String notes;

    @Column(name = "opened_by", nullable = false, updatable = false)
    private Long openedBy;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "closed_by")
    private Long closedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @OneToMany(mappedBy = "stockCount", cascade = CascadeType.ALL)
    private List<StockCountLine> lines = new ArrayList<>();

    @Version
    private Long version;

    protected StockCount() {
    }

    public StockCount(Long warehouseId, String notes, Long openedBy) {
        this.warehouseId = warehouseId;
        this.notes = notes;
        this.openedBy = openedBy;
        this.openedAt = Instant.now();
    }

    public void ensureNotClosed() {
        if (status == StockCountStatus.CLOSED) {
            throw new ConflictException("El recuento " + id + " ya está cerrado");
        }
    }

    public StockCountLine lineFor(Long stockItemId) {
        Optional<StockCountLine> existing = lines.stream()
                .filter(line -> line.getStockItemId().equals(stockItemId))
                .findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }
        StockCountLine line = new StockCountLine(this, stockItemId);
        lines.add(line);
        return line;
    }

    public void markInProgress() {
        if (status == StockCountStatus.OPEN) {
            status = StockCountStatus.IN_PROGRESS;
        }
    }

    public void close(Long userId) {
        ensureNotClosed();
        this.status = StockCountStatus.CLOSED;
        this.closedBy = userId;
        this.closedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public StockCountStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public Long getOpenedBy() {
        return openedBy;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Long getClosedBy() {
        return closedBy;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public List<StockCountLine> getLines() {
        return lines;
    }
}
