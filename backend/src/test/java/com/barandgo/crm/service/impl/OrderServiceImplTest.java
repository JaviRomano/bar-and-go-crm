package com.barandgo.crm.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.barandgo.crm.dto.order.OrderItemRequestDTO;
import com.barandgo.crm.dto.order.OrderRequestDTO;
import com.barandgo.crm.dto.order.OrderResponseDTO;
import com.barandgo.crm.entity.Order;
import com.barandgo.crm.entity.Product;
import com.barandgo.crm.entity.User;
import com.barandgo.crm.enums.PaymentMethod;
import com.barandgo.crm.enums.UserRole;
import com.barandgo.crm.exception.BadRequestException;
import com.barandgo.crm.exception.ForbiddenException;
import com.barandgo.crm.repository.OrderRepository;
import com.barandgo.crm.repository.ProductRepository;
import com.barandgo.crm.repository.UserRepository;
import com.barandgo.crm.security.CurrentUser;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createOrderTakesNameAndPriceFromCatalog() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product(10L, "Tiramisú", "5.50", true)));
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
    void createOrderRejectsUnavailableProduct() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product(10L, "Tiramisú", "5.50", false)));

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
