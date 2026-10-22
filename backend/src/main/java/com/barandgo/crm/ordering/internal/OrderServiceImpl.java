package com.barandgo.crm.ordering.internal;

import com.barandgo.crm.ordering.OrderItemDTO;
import com.barandgo.crm.ordering.OrderItemRequestDTO;
import com.barandgo.crm.ordering.OrderRequestDTO;
import com.barandgo.crm.ordering.OrderResponseDTO;
import com.barandgo.crm.ordering.OrderUpdateStatusDTO;
import com.barandgo.crm.catalog.Product;
import com.barandgo.crm.catalog.ProductService;
import com.barandgo.crm.identity.User;
import com.barandgo.crm.identity.UserDirectory;
import com.barandgo.crm.ordering.OrderStatus;
import com.barandgo.crm.shared.BadRequestException;
import com.barandgo.crm.shared.ConflictException;
import com.barandgo.crm.shared.ForbiddenException;
import com.barandgo.crm.identity.UserRole;
import com.barandgo.crm.ordering.OrderEvents.OrderDelivered;
import com.barandgo.crm.ordering.OrderEvents.OrderDeliveryReverted;
import com.barandgo.crm.shared.ResourceNotFoundException;
import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.ordering.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final UserDirectory userDirectory;
    private final ProductService productService;
    private final ApplicationEventPublisher events;

    public OrderServiceImpl(OrderRepository orderRepository, UserDirectory userDirectory,
                            ProductService productService, ApplicationEventPublisher events) {
        this.orderRepository = orderRepository;
        this.userDirectory = userDirectory;
        this.productService = productService;
        this.events = events;
    }

    @Override
    public OrderResponseDTO createOrder(Long userId, OrderRequestDTO orderRequestDTO) {
        User user = userDirectory.getUser(userId);

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
            // La carta decide si el producto se puede vender (existe, activo y disponible)
            Product product = productService.getSellableProduct(itemDTO.getProductId());

            // Nombre y precio se copian de la carta: el cliente no puede fijarlos
            OrderItem item = new OrderItem();
            item.setProduct(product);
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

        if (!requester.isBackOffice() && !order.getUser().getId().equals(requester.id())) {
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
    public OrderResponseDTO updateOrderStatus(Long id, OrderUpdateStatusDTO orderUpdateStatusDTO, CurrentUser requester) {
        Order order = findOrder(id);
        OrderStatus current = order.getStatus();
        OrderStatus target = orderUpdateStatusDTO.getStatus();
        if (current == target) {
            return convertToResponseDTO(order);
        }
        if (!current.canTransitionTo(target)) {
            throw new ConflictException("Un pedido en estado " + current + " no puede pasar a " + target);
        }
        boolean revertsDelivery = current == OrderStatus.DELIVERED;
        if (revertsDelivery && requester.role() != UserRole.ADMIN && requester.role() != UserRole.MANAGER) {
            throw new ForbiddenException("Solo un encargado puede cancelar un pedido ya entregado");
        }

        order.setStatus(target);
        // Los eventos se guardan en la misma transacción que el cambio de estado (registro de Modulith):
        // si el descuento de stock falla, queda pendiente y se reintenta
        if (target == OrderStatus.DELIVERED) {
            Instant now = Instant.now();
            order.markDelivered(now);
            events.publishEvent(new OrderDelivered(order.getId(), requester.id(), now, order.getItems().stream()
                    .map(item -> new OrderDelivered.Line(
                            item.getProduct() != null ? item.getProduct().getId() : null, item.getQuantity()))
                    .toList()));
        } else if (revertsDelivery) {
            events.publishEvent(new OrderDeliveryReverted(order.getId(), requester.id()));
        }
        Order updatedOrder = orderRepository.save(order);

        logger.info("Pedido {}: {} -> {}", id, current, target);

        return convertToResponseDTO(updatedOrder);
    }

    // Un pedido entregado ha movido stock: se conserva como documento de origen (D8)
    @Override
    public void deleteOrder(Long id) {
        Order order = findOrder(id);
        if (order.getDeliveredAt() != null) {
            throw new ConflictException("El pedido " + id + " se entregó y ha movido stock: no se borra, se cancela");
        }

        orderRepository.delete(order);
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
        if (!userDirectory.exists(userId)) {
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
        // getId() en un proxy LAZY no dispara la carga del producto
        dto.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
        dto.setProductName(item.getProductName());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        return dto;
    }
}
