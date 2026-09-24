package com.barandgo.crm.service;

import com.barandgo.crm.dto.order.OrderRequestDTO;
import com.barandgo.crm.dto.order.OrderResponseDTO;
import com.barandgo.crm.dto.order.OrderUpdateStatusDTO;
import com.barandgo.crm.enums.OrderStatus;
import com.barandgo.crm.security.CurrentUser;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {
    
    OrderResponseDTO createOrder(Long userId, OrderRequestDTO orderRequestDTO);
    
    OrderResponseDTO getOrderById(Long id, CurrentUser requester);
    
    List<OrderResponseDTO> getAllOrders();
    
    List<OrderResponseDTO> getOrdersBetween(LocalDateTime from, LocalDateTime to);
    
    List<OrderResponseDTO> getUpcomingOrders();
    
    OrderResponseDTO updateOrderStatus(Long id, OrderUpdateStatusDTO orderUpdateStatusDTO);
    
    void deleteOrder(Long id);
    
    List<OrderResponseDTO> getOrdersByUserId(Long userId);
    
    List<OrderResponseDTO> getOrdersByUserIdAndStatus(Long userId, OrderStatus status);
    
    List<OrderResponseDTO> getOrdersByStatus(OrderStatus status);
}
