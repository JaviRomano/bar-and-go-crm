package com.barandgo.crm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.barandgo.crm.catalog.Product;
import com.barandgo.crm.identity.User;
import com.barandgo.crm.identity.UserRole;
import com.barandgo.crm.catalog.internal.ProductCategoryRepository;
import com.barandgo.crm.catalog.internal.ProductRepository;
import com.barandgo.crm.identity.internal.UserRepository;
import com.barandgo.crm.identity.internal.TokenService;
import com.barandgo.crm.masterdata.VatRateService;
import com.barandgo.crm.support.PostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;

/**
 * Flujo completo contra PostgreSQL real con el esquema creado por las migraciones de Flyway
 * (Hibernate solo valida): registro, pedidos, roles de back-office y maestros de compras.
 * Se omite automáticamente si Docker no está disponible.
 */
@PostgresIntegrationTest
class BarAndGoCrmApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private VatRateService vatRateService;

    @Test
    void customerRegistersOrdersAndOnlySeesOwnOrders() throws Exception {
        Long productId = createProduct("Croquetas de Jamón", "8.50").getId();

        String anaToken = register("Ana", "ana@example.com", "+34600000001");
        String luisToken = register("Luis", "luis@example.com", "+34600000002");

        // El precio enviado por el cliente se ignora: el total sale de la carta
        String createdOrder = mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + anaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(productId, 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(17.00))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.items[0].productName").value("Croquetas de Jamón"))
                .andExpect(jsonPath("$.items[0].productId").value(productId))
                .andReturn().getResponse().getContentAsString();
        Integer orderId = JsonPath.read(createdOrder, "$.id");

        mockMvc.perform(get("/api/orders/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + anaToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/orders/" + orderId).header(HttpHeaders.AUTHORIZATION, "Bearer " + luisToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCanReadAnyOrder() throws Exception {
        Long productId = createProduct("Nachos", "7.50").getId();
        String customerToken = register("Eva", "eva@example.com", "+34600000006");

        String createdOrder = mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(productId, 1)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer orderId = JsonPath.read(createdOrder, "$.id");

        mockMvc.perform(get("/api/orders/" + orderId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(UserRole.STAFF, "staff-orders@bar.com")))
                .andExpect(status().isOk());
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

    @Test
    void migrationsSeedMenuCategoriesAndUnits() throws Exception {
        mockMvc.perform(get("/api/product-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("ENTRANTES"))
                .andExpect(jsonPath("$[3].name").value("BEBIDAS"));

        mockMvc.perform(get("/api/units/convert")
                        .param("quantity", "1.5").param("from", "kg").param("to", "g")
                        .header(HttpHeaders.AUTHORIZATION, bearer(UserRole.MANAGER, "manager-units@bar.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(1500));
    }

    @Test
    void managerMaintainsSuppliersAndStaffOnlyReads() throws Exception {
        String manager = bearer(UserRole.MANAGER, "manager-suppliers@bar.com");
        String staff = bearer(UserRole.STAFF, "staff-suppliers@bar.com");
        String supplierJson = """
                {"taxId":"b-1234567 4","legalName":"Distribuciones Norte SL","leadTimeDays":2}
                """;

        // El CIF se guarda normalizado: mayúsculas, sin espacios ni guiones
        mockMvc.perform(post("/api/suppliers")
                        .header(HttpHeaders.AUTHORIZATION, manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(supplierJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taxId").value("B12345674"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(post("/api/suppliers")
                        .header(HttpHeaders.AUTHORIZATION, manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(supplierJson))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/suppliers")
                        .header(HttpHeaders.AUTHORIZATION, manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taxId\":\"B12345670\",\"legalName\":\"Control erróneo SL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.taxId").value("El CIF/NIF no es válido"));

        mockMvc.perform(get("/api/suppliers").header(HttpHeaders.AUTHORIZATION, staff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].legalName", hasItem("Distribuciones Norte SL")));

        mockMvc.perform(post("/api/suppliers")
                        .header(HttpHeaders.AUTHORIZATION, staff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(supplierJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCannotCreateAdmin() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(UserRole.MANAGER, "manager-users@bar.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Nuevo Admin","email":"nuevo-admin@bar.com","password":"secreta1",
                                 "phone":"+34600000010","role":"ADMIN"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(UserRole.MANAGER, "manager-users2@bar.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Camarero","email":"camarero@bar.com","password":"secreta1",
                                 "phone":"+34600000011","role":"STAFF","tags":["FLOOR","BAR"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tags", hasSize(2)));
    }

    @Test
    void deletingProductWithSalesArchivesIt() throws Exception {
        Long soldId = createProduct("Bravas", "6.00").getId();
        Long unsoldId = createProduct("Plato de prueba", "1.00").getId();
        String customerToken = register("Raúl", "raul@example.com", "+34600000007");
        mockMvc.perform(post("/api/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(soldId, 1)))
                .andExpect(status().isCreated());

        String admin = bearer(UserRole.ADMIN, "admin-products@bar.com");
        mockMvc.perform(delete("/api/products/" + soldId).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/products/" + unsoldId).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNoContent());

        assertThat(productRepository.findById(soldId)).get()
                .extracting(Product::getActive).isEqualTo(false);
        assertThat(productRepository.findById(unsoldId)).isEmpty();
        mockMvc.perform(get("/api/products"))
                .andExpect(jsonPath("$[*].id", not(hasItem(soldId.intValue()))));
    }

    @Test
    void concurrentUpdateOfSameProductIsRejected() {
        Long productId = createProduct("Tiramisú", "5.50").getId();

        // Dos lecturas independientes (sin transacción en el test): cada una con su copia y su versión
        Product first = productRepository.findById(productId).orElseThrow();
        Product second = productRepository.findById(productId).orElseThrow();

        first.setPrice(new BigDecimal("6.00"));
        productRepository.save(first);

        second.setPrice(new BigDecimal("4.00"));
        assertThatThrownBy(() -> productRepository.save(second))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
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

    // Usuario de back-office creado directamente en BD, con token emitido para él
    private String bearer(UserRole role, String email) {
        User user = new User();
        user.setName("Usuario " + role);
        user.setEmail(email);
        user.setPassword("no-se-usa");
        user.setPhone("+34600000099");
        user.setRole(role);
        User saved = userRepository.save(user);
        return "Bearer " + tokenService.generateToken(saved.getId(), email, role);
    }

    private static String orderJson(Long productId, int quantity) {
        return """
                {"timeTakeAway":"%s","paymentMethod":"CASH","items":[{"productId":%d,"quantity":%d,"unitPrice":0.01}]}
                """.formatted(LocalDateTime.now().plusHours(2).withNano(0), productId, quantity);
    }

    private Product createProduct(String name, String price) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setCategory(categoryRepository.findByActiveTrueOrderBySortOrderAscNameAsc().get(0));
        product.setVatRateId(vatRateService.getVatRates().get(0).id());
        product.setAvailable(true);
        return productRepository.save(product);
    }
}
