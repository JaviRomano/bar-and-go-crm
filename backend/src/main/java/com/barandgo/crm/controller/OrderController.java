package com.barandgo.crm.controller;

import com.barandgo.crm.dto.order.OrderRequestDTO;
import com.barandgo.crm.dto.order.OrderResponseDTO;
import com.barandgo.crm.dto.order.OrderUpdateStatusDTO;
import com.barandgo.crm.enums.OrderStatus;
import com.barandgo.crm.exception.BadRequestException;
import com.barandgo.crm.security.CurrentUser;
import com.barandgo.crm.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody OrderRequestDTO orderRequestDTO) {
        OrderResponseDTO createdOrder = orderService.createOrder(CurrentUser.from(jwt).id(), orderRequestDTO);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id, CurrentUser.from(jwt)));
    }

    /**
     * Todos los pedidos, o los realizados en un intervalo si se indican from y to (ISO-8601).
     */
    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        if (from == null && to == null) {
            return ResponseEntity.ok(orderService.getAllOrders());
        }
        if (from == null || to == null) {
            throw new BadRequestException("Para filtrar por fechas hay que indicar 'from' y 'to'");
        }
        return ResponseEntity.ok(orderService.getOrdersBetween(from, to));
    }

    @GetMapping("/me")
    public ResponseEntity<List<OrderResponseDTO>> getMyOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) OrderStatus status) {
        Long userId = CurrentUser.from(jwt).id();
        List<OrderResponseDTO> orders = status == null
                ? orderService.getOrdersByUserId(userId)
                : orderService.getOrdersByUserIdAndStatus(userId, status);
        return ResponseEntity.ok(orders);
    }

    /**
     * Pedidos con hora de recogida futura, ordenados por hora (vista de cocina/barra).
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<OrderResponseDTO>> getUpcomingOrders() {
        return ResponseEntity.ok(orderService.getUpcomingOrders());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponseDTO> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderUpdateStatusDTO orderUpdateStatusDTO) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, orderUpdateStatusDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByStatus(@PathVariable OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }
}
