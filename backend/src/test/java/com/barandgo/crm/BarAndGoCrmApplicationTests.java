package com.barandgo.crm;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.barandgo.crm.entity.Product;
import com.barandgo.crm.repository.ProductRepository;
import com.jayway.jsonpath.JsonPath;

/**
 * Flujo completo contra PostgreSQL real: registro, login, pedido y control de acceso.
 * Se omite automáticamente si Docker no está disponible.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class BarAndGoCrmApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void customerRegistersOrdersAndOnlySeesOwnOrders() throws Exception {
        Long productId = createProduct("Croquetas de Jamón", "8.50").getId();

        String anaToken = register("Ana", "ana@example.com", "+34600000001");
        String luisToken = register("Luis", "luis@example.com", "+34600000002");

        // El precio enviado por el cliente se ignora: el total sale de la carta
        String orderJson = """
                {"timeTakeAway":"%s","paymentMethod":"CASH","items":[{"productId":%d,"quantity":2,"unitPrice":0.01}]}
                """.formatted(LocalDateTime.now().plusHours(2).withNano(0), productId);

        String createdOrder = mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + anaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(17.00))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.items[0].productName").value("Croquetas de Jamón"))
                .andReturn().getResponse().getContentAsString();
        Integer orderId = JsonPath.read(createdOrder, "$.id");

        mockMvc.perform(get("/api/orders/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + anaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/orders/" + orderId).header(HttpHeaders.AUTHORIZATION, "Bearer " + luisToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void orderWithoutPaymentMethodIsRejected() throws Exception {
        Long productId = createProduct("Tarta de Queso", "5.50").getId();
        String token = register("Pablo", "pablo@example.com", "+34600000005");
        
        String orderJson = """
                {"timeTakeAway":"%s","items":[{"productId":%d,"quantity":1}]}
                """.formatted(LocalDateTime.now().plusHours(2).withNano(0), productId);
        
        mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.paymentMethod").value("El método de pago es obligatorio"));
    }
    
    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        register("Marta", "marta@example.com", "+34600000003");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"marta@example.com\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email o contraseña incorrectos"));
    }

    @Test
    void registerIgnoresRoleAndCreatesCustomer() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Intruso","email":"intruso@example.com","password":"secreta1",
                                 "phone":"+34600000004","role":"ADMIN"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
    }

    private String register(String name, String email, String phone) throws Exception {
        String body = """
                {"name":"%s","email":"%s","password":"secreta1","phone":"%s"}
                """.formatted(name, email, phone);
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private Product createProduct(String name, String price) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setCategory("ENTRANTES");
        product.setAvailable(true);
        return productRepository.save(product);
    }
}
