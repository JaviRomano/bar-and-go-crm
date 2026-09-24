package com.barandgo.crm.service.impl;

import com.barandgo.crm.dto.order.OrderItemDTO;
import com.barandgo.crm.dto.order.OrderItemRequestDTO;
import com.barandgo.crm.dto.order.OrderRequestDTO;
import com.barandgo.crm.dto.order.OrderResponseDTO;
import com.barandgo.crm.dto.order.OrderUpdateStatusDTO;
import com.barandgo.crm.entity.OrderItem;
import com.barandgo.crm.entity.Order;
import com.barandgo.crm.entity.Product;
import com.barandgo.crm.entity.User;
import com.barandgo.crm.enums.OrderStatus;
import com.barandgo.crm.exception.BadRequestException;
import com.barandgo.crm.exception.ForbiddenException;
import com.barandgo.crm.exception.ResourceNotFoundException;
import com.barandgo.crm.repository.OrderRepository;
import com.barandgo.crm.repository.ProductRepository;
import com.barandgo.crm.repository.UserRepository;
import com.barandgo.crm.security.CurrentUser;
import com.barandgo.crm.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public OrderServiceImpl(OrderRepository orderRepository, UserRepository userRepository,
                            ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Override
    public OrderResponseDTO createOrder(Long userId, OrderRequestDTO orderRequestDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));

        if (orderRequestDTO.getTimeTakeAway().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("La hora de recogida debe ser futura");
        }

        if (orderRequestDTO.getItems() == null || orderRequestDTO.getItems().isEmpty()) {
            throw new BadRequestException("El pedido debe contener al menos un producto");
        }

        Order order = new Order();
        order.setUser(user);
        order.setTimeTakeAway(orderRequestDTO.getTimeTakeAway());
        order.setPaymentMethod(orderRequestDTO.getPaymentMethod());

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequestDTO itemDTO : orderRequestDTO.getItems()) {
            Product product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado con ID: " + itemDTO.getProductId()));

            if (!Boolean.TRUE.equals(product.getAvailable())) {
                throw new BadRequestException("El producto '" + product.getName() + "' no está disponible");
            }

            // Nombre y precio se copian de la carta: el cliente no puede fijarlos
            OrderItem item = new OrderItem();
            item.setProductName(product.getName());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(itemDTO.getQuantity());

            order.addItem(item);
            total = total.add(item.getSubtotal());
        }

        order.setTotal(total);

        Order savedOrder = orderRepository.save(order);
        logger.info("Pedido {} creado por el usuario {} con total {}", savedOrder.getId(), userId, savedOrder.getTotal());

        return convertToResponseDTO(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long id, CurrentUser requester) {
        Order order = findOrder(id);

        if (!requester.isAdmin() && !order.getUser().getId().equals(requester.id())) {
            throw new ForbiddenException("No tienes acceso a este pedido");
        }

        return convertToResponseDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrders() {
        return toResponseList(orderRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersBetween(LocalDateTime from, LocalDateTime to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("La fecha 'from' no puede ser posterior a 'to'");
        }
        return toResponseList(orderRepository.findOrdersBetweenDates(from, to));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getUpcomingOrders() {
        return toResponseList(orderRepository.findFutureOrders(LocalDateTime.now()));
    }

    @Override
    public OrderResponseDTO updateOrderStatus(Long id, OrderUpdateStatusDTO orderUpdateStatusDTO) {
        Order order = findOrder(id);

        order.setStatus(orderUpdateStatusDTO.getStatus());
        Order updatedOrder = orderRepository.save(order);

        logger.info("Pedido {} actualizado a estado {}", id, orderUpdateStatusDTO.getStatus());

        return convertToResponseDTO(updatedOrder);
    }

    @Override
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Pedido no encontrado con ID: " + id);
        }

        orderRepository.deleteById(id);
        logger.info("Pedido {} eliminado", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByUserId(Long userId) {
        ensureUserExists(userId);
        return toResponseList(orderRepository.findByUserId(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByUserIdAndStatus(Long userId, OrderStatus status) {
        ensureUserExists(userId);
        return toResponseList(orderRepository.findByUserIdAndStatus(userId, status));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getOrdersByStatus(OrderStatus status) {
        return toResponseList(orderRepository.findByStatus(status));
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + id));
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + userId);
        }
    }

    private List<OrderResponseDTO> toResponseList(List<Order> orders) {
        return orders.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    private OrderResponseDTO convertToResponseDTO(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUser().getId());
        dto.setUserName(order.getUser().getName());
        dto.setDateOrder(order.getDateOrder());
        dto.setTimeTakeAway(order.getTimeTakeAway());
        dto.setTotal(order.getTotal());
        dto.setStatus(order.getStatus());
        dto.setPaymentMethod(order.getPaymentMethod());

        List<OrderItemDTO> itemDTOs = order.getItems().stream()
                .map(this::convertItemToDTO)
                .collect(Collectors.toList());
        dto.setItems(itemDTOs);

        return dto;
    }

    private OrderItemDTO convertItemToDTO(OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(item.getId());
        dto.setProductName(item.getProductName());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        return dto;
    }
}
