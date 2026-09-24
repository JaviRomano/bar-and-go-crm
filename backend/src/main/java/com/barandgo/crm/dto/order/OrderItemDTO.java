package com.barandgo.crm.dto.order;

import java.math.BigDecimal;

/**
 * Línea de pedido en las respuestas. Nombre y precio son una copia del producto
 * en el momento de la compra, para que cambios posteriores en la carta no alteren pedidos pasados.
 */
public class OrderItemDTO {

    private Long id;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;

    public OrderItemDTO() {
    }

    public BigDecimal getSubtotal() {
        if (unitPrice != null && quantity != null) {
            return unitPrice.multiply(new BigDecimal(quantity));
        }
        return BigDecimal.ZERO;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
