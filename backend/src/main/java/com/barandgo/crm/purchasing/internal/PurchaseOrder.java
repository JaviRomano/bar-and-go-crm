package com.barandgo.crm.purchasing.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.barandgo.crm.purchasing.PurchaseOrderStatus;
import com.barandgo.crm.shared.BadRequestException;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Pedido a proveedor. Las transiciones de estado pasan por {@link #transitionTo}, que aplica
 * las reglas de {@link PurchaseOrderStatus}.
 */
@Entity
@Table(name = "purchase_order")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // PC-<año>-<secuencia>, asignado al crear
    @Column(nullable = false, unique = true, length = 20)
    private String number;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(length = 500)
    private String notes;

    // Id del usuario que lo creó (módulo de identidad)
    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber")
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public PurchaseOrder() {
    }

    public void addLine(PurchaseOrderLine line) {
        lines.add(line);
        line.setPurchaseOrder(this);
    }

    public void ensureEditable() {
        if (status != PurchaseOrderStatus.DRAFT) {
            throw new ConflictException("Solo se puede modificar un pedido en borrador (estado actual: " + status + ")");
        }
    }

    public void transitionTo(PurchaseOrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new ConflictException("Un pedido en estado " + status + " no puede pasar a " + target);
        }
        this.status = target;
    }

    public void send() {
        if (lines.isEmpty()) {
            throw new BadRequestException("No se puede enviar un pedido sin líneas");
        }
        transitionTo(PurchaseOrderStatus.SENT);
        this.sentAt = Instant.now();
    }

    public void cancel() {
        transitionTo(PurchaseOrderStatus.CANCELLED);
        this.cancelledAt = Instant.now();
    }

    /**
     * Suma lo recibido a cada línea (en la unidad de la línea) y pasa a RECEIVED si todas están
     * completas o a PARTIALLY_RECEIVED si no. Se admite recibir de más: el proveedor manda lo que manda.
     */
    public void registerReceipt(Map<Long, BigDecimal> receivedByLineId) {
        if (status != PurchaseOrderStatus.SENT && status != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new ConflictException("Solo se recibe mercancía de un pedido enviado (estado actual: " + status + ")");
        }
        receivedByLineId.forEach((lineId, quantity) -> lines.stream()
                .filter(line -> line.getId().equals(lineId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("La línea " + lineId + " no pertenece al pedido " + number))
                .addReceived(quantity));

        boolean complete = lines.stream().allMatch(PurchaseOrderLine::isFullyReceived);
        PurchaseOrderStatus target = complete ? PurchaseOrderStatus.RECEIVED : PurchaseOrderStatus.PARTIALLY_RECEIVED;
        if (target != status) {
            transitionTo(target);
        }
    }

    /** Da por recibido un pedido aunque falte mercancía (el proveedor no mandará el resto). */
    public void close() {
        if (status == PurchaseOrderStatus.DRAFT) {
            throw new ConflictException("Un borrador no se cierra: se envía o se borra");
        }
        transitionTo(PurchaseOrderStatus.RECEIVED);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public PurchaseOrderStatus getStatus() {
        return status;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public LocalDate getExpectedDate() {
        return expectedDate;
    }

    public void setExpectedDate(LocalDate expectedDate) {
        this.expectedDate = expectedDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public List<PurchaseOrderLine> getLines() {
        return lines;
    }

    public Long getVersion() {
        return version;
    }
}
