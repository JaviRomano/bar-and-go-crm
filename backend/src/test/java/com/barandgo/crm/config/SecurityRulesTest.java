package com.barandgo.crm.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.barandgo.crm.dto.auth.AuthResponseDTO;
import com.barandgo.crm.dto.order.OrderResponseDTO;
import com.barandgo.crm.enums.UserRole;
import com.barandgo.crm.security.TokenService;
import com.barandgo.crm.service.AuthService;
import com.barandgo.crm.service.OrderService;
import com.barandgo.crm.service.ProductService;
import com.barandgo.crm.service.ReservationService;
import com.barandgo.crm.service.UserService;

@WebMvcTest
@ActiveProfiles("test")
@Import({SecurityConfig.class, JwtConfig.class, TokenService.class})
class SecurityRulesTest {

    private static final String FRONTEND_ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private ReservationService reservationService;

    @Test
    void catalogIsPublic() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of());

        mockMvc.perform(get("/api/products")).andExpect(status().isOk());
    }

    @Test
    void loginIsPublic() throws Exception {
        when(authService.login(any())).thenReturn(new AuthResponseDTO());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@bar.com\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/orders/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void malformedTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/orders/me").header(HttpHeaders.AUTHORIZATION, "Bearer no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotManageUsers() throws Exception {
        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(UserRole.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanManageUsers() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(UserRole.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void customerCannotModifyCatalog() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(UserRole.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Gratis\",\"price\":0.01,\"category\":\"POSTRES\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCanReadOwnOrdersAndOrderById() throws Exception {
        when(orderService.getOrdersByUserId(1L)).thenReturn(List.of());
        when(orderService.getOrderById(any(), any())).thenReturn(new OrderResponseDTO());

        mockMvc.perform(get("/api/orders/me").header(HttpHeaders.AUTHORIZATION, bearer(UserRole.CUSTOMER)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/orders/5").header(HttpHeaders.AUTHORIZATION, bearer(UserRole.CUSTOMER)))
                .andExpect(status().isOk());
    }

    @Test
    void customerCannotUseAdminOrderViews() throws Exception {
        // "upcoming" no debe colarse por la regla de /api/orders/{id}
        mockMvc.perform(get("/api/orders/upcoming").header(HttpHeaders.AUTHORIZATION, bearer(UserRole.CUSTOMER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/orders").header(HttpHeaders.AUTHORIZATION, bearer(UserRole.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotListAllReservations() throws Exception {
        mockMvc.perform(get("/api/reservations").header(HttpHeaders.AUTHORIZATION, bearer(UserRole.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void corsPreflightAllowsConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/orders/me")
                        .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN));
    }

    @Test
    void corsPreflightRejectsUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/orders/me")
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    private String bearer(UserRole role) {
        return "Bearer " + tokenService.generateToken(1L, role.name().toLowerCase() + "@example.com", role);
    }
}
