package com.barandgo.crm.recipes.internal;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Coste de un escandallo en un momento dado. Se añade una fila cuando el coste cambia.
 */
@Entity
@Immutable
@Table(name = "recipe_cost_history")
public class RecipeCostHistory {

    enum Reason {
        CREATED,
        UPDATED,
        PRICE_CHANGE,
        RECALCULATION
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Column(name = "total_cost", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalCost;

    // Por ración (DISH) o por unidad de rendimiento (PREPARATION)
    @Column(name = "cost_per_unit", nullable = false, precision = 19, scale = 6)
    private BigDecimal costPerUnit;

    @Column(nullable = false)
    private Boolean complete;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Reason reason;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected RecipeCostHistory() {
    }

    RecipeCostHistory(Long recipeId, BigDecimal totalCost, BigDecimal costPerUnit, boolean complete, Reason reason) {
        this.recipeId = recipeId;
        this.totalCost = totalCost;
        this.costPerUnit = costPerUnit;
        this.complete = complete;
        this.reason = reason;
        this.recordedAt = Instant.now();
    }

    public Long getRecipeId() {
        return recipeId;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public BigDecimal getCostPerUnit() {
        return costPerUnit;
    }

    public Boolean getComplete() {
        return complete;
    }

    public Reason getReason() {
        return reason;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
