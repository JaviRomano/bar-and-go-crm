package com.barandgo.crm.dto.order;

import com.barandgo.crm.enums.OrderStatus;
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