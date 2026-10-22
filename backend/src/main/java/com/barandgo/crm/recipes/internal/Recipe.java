package com.barandgo.crm.recipes.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.barandgo.crm.recipes.RecipeType;

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
 * Escandallo. Producto, artículo de stock y unidades son de otros módulos y se guardan por id.
 */
@Entity
@Table(name = "recipe")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private RecipeType type;

    // Producto de la carta (DISH)
    @Column(name = "product_id", unique = true)
    private Long productId;

    // Artículo de stock de una elaboración que se produce y se guarda (PREPARATION inventariable)
    @Column(name = "stock_item_id", unique = true)
    private Long stockItemId;

    @Column(precision = 10, scale = 2)
    private BigDecimal portions;

    @Column(name = "yield_quantity", precision = 18, scale = 4)
    private BigDecimal yieldQuantity;

    @Column(name = "yield_unit_id")
    private Long yieldUnitId;

    @Column(name = "target_food_cost_pct", precision = 5, scale = 2)
    private BigDecimal targetFoodCostPct;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber")
    private List<RecipeLine> lines = new ArrayList<>();

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    protected Recipe() {
    }

    public Recipe(RecipeType type) {
        this.type = type;
    }

    public void replaceLines(List<RecipeLine> newLines) {
        lines.clear();
        newLines.forEach(line -> {
            line.setRecipe(this);
            lines.add(line);
        });
    }

    /** Raciones (DISH) o cantidad que rinde (PREPARATION). */
    public BigDecimal outputQuantity() {
        return type == RecipeType.DISH ? portions : yieldQuantity;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RecipeType getType() {
        return type;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getStockItemId() {
        return stockItemId;
    }

    public void setStockItemId(Long stockItemId) {
        this.stockItemId = stockItemId;
    }

    public BigDecimal getPortions() {
        return portions;
    }

    public void setPortions(BigDecimal portions) {
        this.portions = portions;
    }

    public BigDecimal getYieldQuantity() {
        return yieldQuantity;
    }

    public void setYieldQuantity(BigDecimal yieldQuantity) {
        this.yieldQuantity = yieldQuantity;
    }

    public Long getYieldUnitId() {
        return yieldUnitId;
    }

    public void setYieldUnitId(Long yieldUnitId) {
        this.yieldUnitId = yieldUnitId;
    }

    public BigDecimal getTargetFoodCostPct() {
        return targetFoodCostPct;
    }

    public void setTargetFoodCostPct(BigDecimal targetFoodCostPct) {
        this.targetFoodCostPct = targetFoodCostPct;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public List<RecipeLine> getLines() {
        return lines;
    }

    public Long getVersion() {
        return version;
    }
}
