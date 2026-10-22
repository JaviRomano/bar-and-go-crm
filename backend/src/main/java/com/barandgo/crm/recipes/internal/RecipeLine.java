package com.barandgo.crm.recipes.internal;

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
 * Ingrediente de un escandallo: un artículo de compra o una elaboración (sub-escandallo).
 */
@Entity
@Table(name = "recipe_line")
public class RecipeLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "stock_item_id")
    private Long stockItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_recipe_id")
    private Recipe subRecipe;

    @Column(name = "gross_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal grossQuantity;

    @Column(name = "unit_id", nullable = false)
    private Long unitId;

    @Column(name = "yield_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal yieldPct;

    protected RecipeLine() {
    }

    public RecipeLine(Integer lineNumber, Long stockItemId, Recipe subRecipe, BigDecimal grossQuantity, Long unitId,
                      BigDecimal yieldPct) {
        this.lineNumber = lineNumber;
        this.stockItemId = stockItemId;
        this.subRecipe = subRecipe;
        this.grossQuantity = grossQuantity;
        this.unitId = unitId;
        this.yieldPct = yieldPct;
    }

    void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public Long getId() {
        return id;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public Long getStockItemId() {
        return stockItemId;
    }

    public Recipe getSubRecipe() {
        return subRecipe;
    }

    public BigDecimal getGrossQuantity() {
        return grossQuantity;
    }

    public Long getUnitId() {
        return unitId;
    }

    public BigDecimal getYieldPct() {
        return yieldPct;
    }
}
