package com.barandgo.crm.recipes.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Producción de una elaboración: qué se hizo, dónde, cuánto y a qué coste real. Los movimientos de
 * stock correspondientes comparten {@code operationId}.
 */
@Entity
@Immutable
@Table(name = "production_batch")
public class ProductionBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_id", nullable = false)
    private Long unitId;

    @Column(name = "total_cost", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalCost;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 6)
    private BigDecimal unitCost;

    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    @Column(length = 255)
    private String notes;

    @Column(name = "produced_by", nullable = false)
    private Long producedBy;

    @Column(name = "produced_at", nullable = false)
    private Instant producedAt;

    protected ProductionBatch() {
    }

    ProductionBatch(Long recipeId, Long warehouseId, BigDecimal quantity, Long unitId, BigDecimal totalCost,
                    BigDecimal unitCost, UUID operationId, String notes, Long producedBy) {
        this.recipeId = recipeId;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
        this.unitId = unitId;
        this.totalCost = totalCost;
        this.unitCost = unitCost;
        this.operationId = operationId;
        this.notes = notes;
        this.producedBy = producedBy;
        this.producedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }
}
