package com.barandgo.crm.ordering;

import com.barandgo.crm.identity.CurrentUser;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {
    
    OrderResponseDTO createOrder(Long userId, OrderRequestDTO orderRequestDTO);
    
    OrderResponseDTO getOrderById(Long id, CurrentUser requester);
    
    List<OrderResponseDTO> getAllOrders();
    
    List<OrderResponseDTO> getOrdersBetween(LocalDateTime from, LocalDateTime to);
    
    List<OrderResponseDTO> getUpcomingOrders();

    /** Ventas para informes: líneas de pedidos entregados en [from, to) que siguen entregados. */
    List<SoldLine> getDeliveredSales(Instant from, Instant to);
    
    /**
     * Cambia el estado según {@link OrderStatus#canTransitionTo}. Entregar publica OrderDelivered;
     * cancelar un pedido entregado (solo ADMIN o MANAGER) publica OrderDeliveryReverted.
     */
    OrderResponseDTO updateOrderStatus(Long id, OrderUpdateStatusDTO orderUpdateStatusDTO, CurrentUser requester);
    
    void deleteOrder(Long id);
    
    List<OrderResponseDTO> getOrdersByUserId(Long userId);
    
    List<OrderResponseDTO> getOrdersByUserIdAndStatus(Long userId, OrderStatus status);
    
    List<OrderResponseDTO> getOrdersByStatus(OrderStatus status);
}
