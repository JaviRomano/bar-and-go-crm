package com.barandgo.crm.ordering;

import jakarta.validation.constraints.NotNull;

public class OrderUpdateStatusDTO {
    
    @NotNull(message = "El estado es obligatorio")
    private OrderStatus status;
    
    public OrderUpdateStatusDTO() {
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}