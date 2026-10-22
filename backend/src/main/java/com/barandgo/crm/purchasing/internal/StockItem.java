package com.barandgo.crm.purchasing.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import com.barandgo.crm.masterdata.Allergen;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Artículo de compra (materia prima o mercancía). Distinto de un producto de la carta: el
 * escandallo (Fase 4) relaciona los dos.
 *
 * <p>Categoría, unidades, proveedor habitual e IVA son datos maestros de otro módulo y se
 * guardan por id; la base de datos garantiza que existen.
 */
@Entity
@Table(name = "stock_item")
public class StockItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "item_category_id", nullable = false)
    private Long itemCategoryId;

    // Unidad en la que se compra (caja), en la que se cuenta el stock (ud) y en la que se usa en cocina (ud, g...)
    @Column(name = "purchase_unit_id", nullable = false)
    private Long purchaseUnitId;

    @Column(name = "stock_unit_id", nullable = false)
    private Long stockUnitId;

    @Column(name = "usage_unit_id", nullable = false)
    private Long usageUnitId;

    @ElementCollection
    @CollectionTable(name = "stock_item_unit_conversion", joinColumns = @JoinColumn(name = "stock_item_id"))
    private Set<ItemUnitConversion> conversions = new HashSet<>();

    @Column(name = "preferred_supplier_id")
    private Long preferredSupplierId;

    @Column(name = "vat_rate_id", nullable = false)
    private Long vatRateId;

    // En unidad de inventario
    @Column(name = "min_stock", precision = 18, scale = 4)
    private BigDecimal minStock;

    @Column(name = "max_stock", precision = 18, scale = 4)
    private BigDecimal maxStock;

    @ElementCollection
    @CollectionTable(name = "stock_item_allergen", joinColumns = @JoinColumn(name = "stock_item_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "allergen", nullable = false, length = 20)
    private Set<Allergen> allergens = EnumSet.noneOf(Allergen.class);

    @Column(nullable = false)
    private Boolean perishable = false;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public StockItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getItemCategoryId() {
        return itemCategoryId;
    }

    public void setItemCategoryId(Long itemCategoryId) {
        this.itemCategoryId = itemCategoryId;
    }

    public Long getPurchaseUnitId() {
        return purchaseUnitId;
    }

    public void setPurchaseUnitId(Long purchaseUnitId) {
        this.purchaseUnitId = purchaseUnitId;
    }

    public Long getStockUnitId() {
        return stockUnitId;
    }

    public void setStockUnitId(Long stockUnitId) {
        this.stockUnitId = stockUnitId;
    }

    public Long getUsageUnitId() {
        return usageUnitId;
    }

    public void setUsageUnitId(Long usageUnitId) {
        this.usageUnitId = usageUnitId;
    }

    public Set<ItemUnitConversion> getConversions() {
        return conversions;
    }

    // Se modifica la colección gestionada en lugar de sustituirla
    public void setConversions(Set<ItemUnitConversion> conversions) {
        this.conversions.clear();
        if (conversions != null) {
            this.conversions.addAll(conversions);
        }
    }

    public Long getPreferredSupplierId() {
        return preferredSupplierId;
    }

    public void setPreferredSupplierId(Long preferredSupplierId) {
        this.preferredSupplierId = preferredSupplierId;
    }

    public Long getVatRateId() {
        return vatRateId;
    }

    public void setVatRateId(Long vatRateId) {
        this.vatRateId = vatRateId;
    }

    public BigDecimal getMinStock() {
        return minStock;
    }

    public void setMinStock(BigDecimal minStock) {
        this.minStock = minStock;
    }

    public BigDecimal getMaxStock() {
        return maxStock;
    }

    public void setMaxStock(BigDecimal maxStock) {
        this.maxStock = maxStock;
    }

    public Set<Allergen> getAllergens() {
        return allergens;
    }

    public void setAllergens(Set<Allergen> allergens) {
        this.allergens.clear();
        if (allergens != null) {
            this.allergens.addAll(allergens);
        }
    }

    public Boolean getPerishable() {
        return perishable;
    }

    public void setPerishable(Boolean perishable) {
        this.perishable = perishable;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getVersion() {
        return version;
    }
}
