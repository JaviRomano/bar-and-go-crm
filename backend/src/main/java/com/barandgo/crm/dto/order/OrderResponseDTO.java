package com.barandgo.crm.dto.order;

import com.barandgo.crm.enums.OrderStatus;
import com.barandgo.crm.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderResponseDTO {
    
    private Long id;
    private Long userId;
    private String userName;
    private LocalDateTime dateOrder;
    private LocalDateTime timeTakeAway;
    private BigDecimal total;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private List<OrderItemDTO> items = new ArrayList<>();
    
    public OrderResponseDTO() {
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public LocalDateTime getDateOrder() {
        return dateOrder;
    }
    
    public void setDateOrder(LocalDateTime dateOrder) {
        this.dateOrder = dateOrder;
    }
    
    public LocalDateTime getTimeTakeAway() {
        return timeTakeAway;
    }
    
    public void setTimeTakeAway(LocalDateTime timeTakeAway) {
        this.timeTakeAway = timeTakeAway;
    }
    
    public BigDecimal getTotal() {
        return total;
    }
    
    public void setTotal(BigDecimal total) {
        this.total = total;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public List<OrderItemDTO> getItems() {
        return items;
    }
    
    public void setItems(List<OrderItemDTO> items) {
        this.items = items;
    }
}