package com.barandgo.crm.catalog;

import java.math.BigDecimal;
import java.util.Set;

import com.barandgo.crm.masterdata.Allergen;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProductDTO {
	   
    private Long id;
    
    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
    private String name;
    
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String description;
    
    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor que 0")
    private BigDecimal price;
    
    @NotNull(message = "La categoría es obligatoria")
    private Long categoryId;

    // Solo en respuestas: nombre de la categoría, para mostrar y filtrar sin otra petición
    private String category;

    // Opcional: por defecto, el tipo de IVA del 10 %
    private Long vatRateId;

    // Solo en respuestas: porcentaje de IVA incluido en el precio
    private BigDecimal vatRate;

    // Solo en respuestas: alérgenos calculados desde el escandallo (Reglamento UE 1169/2011)
    private Set<Allergen> allergens;
    
    private Boolean available = true;
    
    @Size(max = 500, message = "La URL de la imagen no puede exceder 500 caracteres")
    private String imageUrl;
    
    public ProductDTO() {
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public Long getVatRateId() {
        return vatRateId;
    }

    public void setVatRateId(Long vatRateId) {
        this.vatRateId = vatRateId;
    }

    public BigDecimal getVatRate() {
        return vatRate;
    }

    public void setVatRate(BigDecimal vatRate) {
        this.vatRate = vatRate;
    }

    public Set<Allergen> getAllergens() {
        return allergens;
    }

    public void setAllergens(Set<Allergen> allergens) {
        this.allergens = allergens;
    }

    public Boolean getAvailable() {
        return available;
    }
    
    public void setAvailable(Boolean available) {
        this.available = available;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
