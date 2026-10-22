package com.barandgo.crm.ordering.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.barandgo.crm.ordering.OrderItemRequestDTO;
import com.barandgo.crm.ordering.OrderRequestDTO;
import com.barandgo.crm.ordering.OrderResponseDTO;
import com.barandgo.crm.catalog.Product;
import com.barandgo.crm.identity.User;
import com.barandgo.crm.ordering.PaymentMethod;
import com.barandgo.crm.identity.UserRole;
import com.barandgo.crm.shared.BadRequestException;
import com.barandgo.crm.shared.ForbiddenException;
import com.barandgo.crm.catalog.ProductService;
import com.barandgo.crm.identity.UserDirectory;
import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.ordering.OrderEvents.OrderDelivered;
import com.barandgo.crm.ordering.OrderEvents.OrderDeliveryReverted;
import com.barandgo.crm.ordering.OrderStatus;
import com.barandgo.crm.ordering.OrderUpdateStatusDTO;
import com.barandgo.crm.shared.ConflictException;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserDirectory userDirectory;

    @Mock
    private ProductService productService;

    @Mock
    private ApplicationEventPublisher events;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createOrderTakesNameAndPriceFromCatalog() {
        when(userDirectory.getUser(1L)).thenReturn(user(1L));
        when(productService.getSellableProduct(10L)).thenReturn(product(10L, "Tiramisú", "5.50", true));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponseDTO response = orderService.createOrder(1L, request(10L, 3));

        assertThat(response.getTotal()).isEqualByComparingTo("16.50");
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(response.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getProductName()).isEqualTo("Tiramisú");
            assertThat(item.getUnitPrice()).isEqualByComparingTo("5.50");
            assertThat(item.getQuantity()).isEqualTo(3);
        });
    }

    @Test
    void createOrderStopsWhenCatalogRejectsProduct() {
        // La regla de disponibilidad es de la carta; pedidos solo debe no guardar nada
        when(userDirectory.getUser(1L)).thenReturn(user(1L));
        when(productService.getSellableProduct(10L))
                .thenThrow(new BadRequestException("El producto 'Tiramisú' no está disponible"));

        assertThatThrownBy(() -> orderService.createOrder(1L, request(10L, 1)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no está disponible");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void customerCannotReadAnotherCustomersOrder() {
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order(50L, user(2L))));

        CurrentUser requester = new CurrentUser(1L, "ana@example.com", UserRole.CUSTOMER);

        assertThatThrownBy(() -> orderService.getOrderById(50L, requester))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void adminCanReadAnyOrder() {
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order(50L, user(2L))));

        CurrentUser admin = new CurrentUser(99L, "admin@bar.com", UserRole.ADMIN);

        assertThat(orderService.getOrderById(50L, admin).getUserId()).isEqualTo(2L);
    }

    @Test
    void ordersBetweenRejectsInvertedRange() {
        LocalDateTime now = LocalDateTime.now();

        assertThatThrownBy(() -> orderService.getOrdersBetween(now, now.minusDays(1)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void deliveringPublishesEventWithLines() {
        Order order = order(50L, user(2L));
        order.setStatus(OrderStatus.READY);
        OrderItem item = new OrderItem();
        item.setProduct(product(10L, "Tiramisú", "5.50", true));
        item.setProductName("Tiramisú");
        item.setUnitPrice(new BigDecimal("5.50"));
        item.setQuantity(2);
        order.addItem(item);
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.updateOrderStatus(50L, status(OrderStatus.DELIVERED), STAFF);

        assertThat(order.getDeliveredAt()).isNotNull();
        verify(events).publishEvent(argThat((Object event) -> event instanceof OrderDelivered delivered
                && delivered.orderId().equals(50L)
                && delivered.lines().equals(List.of(new OrderDelivered.Line(10L, 2)))));
    }

    @Test
    void skippingStatesIsRejected() {
        Order order = order(50L, user(2L));
        order.setStatus(OrderStatus.CREATED);
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(50L, status(OrderStatus.DELIVERED), STAFF))
                .isInstanceOf(ConflictException.class);
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void sameStatusIsANoOp() {
        Order order = order(50L, user(2L));
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        orderService.updateOrderStatus(50L, status(OrderStatus.DELIVERED), STAFF);

        verify(events, never()).publishEvent(any(Object.class));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void onlyManagersCancelDeliveredOrders() {
        Order order = order(50L, user(2L));
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(50L, status(OrderStatus.CANCELLED), STAFF))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cancellingDeliveredOrderPublishesReversal() {
        Order order = order(50L, user(2L));
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CurrentUser manager = new CurrentUser(7L, "encargado@bar.com", UserRole.MANAGER);
        orderService.updateOrderStatus(50L, status(OrderStatus.CANCELLED), manager);

        verify(events).publishEvent(new OrderDeliveryReverted(50L, 7L));
    }

    @Test
    void deliveredOrderCannotBeDeleted() {
        Order order = order(50L, user(2L));
        order.markDelivered(Instant.now());
        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.deleteOrder(50L)).isInstanceOf(ConflictException.class);
        verify(orderRepository, never()).delete(any());
    }

    private static final CurrentUser STAFF = new CurrentUser(3L, "personal@bar.com", UserRole.STAFF);

    private static OrderUpdateStatusDTO status(OrderStatus status) {
        OrderUpdateStatusDTO dto = new OrderUpdateStatusDTO();
        dto.setStatus(status);
        return dto;
    }

    private static OrderRequestDTO request(Long productId, int quantity) {
        OrderItemRequestDTO item = new OrderItemRequestDTO();
        item.setProductId(productId);
        item.setQuantity(quantity);

        OrderRequestDTO request = new OrderRequestDTO();
        request.setTimeTakeAway(LocalDateTime.now().plusHours(1));
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setItems(List.of(item));
        return request;
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("Usuario " + id);
        user.setRole(UserRole.CUSTOMER);
        return user;
    }

    private static Product product(Long id, String name, String price, boolean available) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setAvailable(available);
        return product;
    }

    private static Order order(Long id, User owner) {
        Order order = new Order();
        order.setId(id);
        order.setUser(owner);
        order.setTotal(BigDecimal.TEN);
        return order;
    }
}
