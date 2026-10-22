package com.barandgo.crm.purchasing;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.barandgo.crm.identity.User;
import com.barandgo.crm.identity.UserRole;
import com.barandgo.crm.identity.internal.TokenService;
import com.barandgo.crm.identity.internal.UserRepository;
import com.jayway.jsonpath.JsonPath;

/**
 * Compras de principio a fin contra PostgreSQL con el esquema de Flyway: categoría, proveedor,
 * artículo con conversión propia, tarifa, pedido y su ciclo de vida. Se omite sin Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PurchasingFlowTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenService tokenService;

    private String manager;
    private String staff;

    @BeforeEach
    void setUp() {
        manager = bearer(UserRole.MANAGER);
        staff = bearer(UserRole.STAFF);
    }

    @Test
    void fromStockItemToPurchaseOrderLifecycle() throws Exception {
        long categoryId = idOf(perform(post("/api/item-categories"), manager, "{\"name\":\"Bebidas\"}")
                .andExpect(status().isCreated()));
        long supplierId = idOf(perform(post("/api/suppliers"), manager,
                "{\"taxId\":\"A58818501\",\"legalName\":\"Bebidas del Norte SA\",\"leadTimeDays\":2}")
                .andExpect(status().isCreated()));
        long reducedVatId = vatRateId("Reducido");

        String water = """
                {"code":"agua-50","name":"Agua 50 cl","itemCategoryId":%d,"purchaseUnit":"caja","stockUnit":"ud",
                 "usageUnit":"ud","vatRateId":%d,"minStock":24,"maxStock":96,%s}
                """;
        // Sin decir cuántas unidades trae la caja no se puede pasar a stock
        perform(post("/api/stock-items"), manager, water.formatted(categoryId, reducedVatId, "\"conversions\":[]"))
                .andExpect(status().isBadRequest());

        long waterId = idOf(perform(post("/api/stock-items"), manager, water.formatted(categoryId, reducedVatId,
                "\"conversions\":[{\"fromUnit\":\"caja\",\"toUnit\":\"ud\",\"factor\":24}]"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("AGUA-50"))
                .andExpect(jsonPath("$.itemCategory").value("Bebidas")));

        perform(put("/api/stock-items/" + waterId + "/suppliers/" + supplierId), manager,
                "{\"supplierItemCode\":\"AG50-24\",\"purchaseUnit\":\"caja\",\"unitPrice\":7.20}")
                .andExpect(status().isOk());

        // El personal ve el artículo pero no la tarifa
        mockMvc.perform(get("/api/stock-items/" + waterId).header(HttpHeaders.AUTHORIZATION, staff))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/stock-items/" + waterId + "/suppliers").header(HttpHeaders.AUTHORIZATION, staff))
                .andExpect(status().isForbidden());

        String order = "{\"supplierId\":%d,\"lines\":[{\"stockItemId\":%d,\"quantity\":%s%s}]}";
        long orderId = idOf(perform(post("/api/purchase-orders"), manager, order.formatted(supplierId, waterId, "2", ""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value(startsWith("PC-" + LocalDate.now().getYear() + "-")))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.expectedDate").value(LocalDate.now().plusDays(2).toString()))
                .andExpect(jsonPath("$.lines[0].unit").value("caja"))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(7.2))
                .andExpect(jsonPath("$.totalBase").value(14.4))
                .andExpect(jsonPath("$.totalVat").value(1.44))
                .andExpect(jsonPath("$.total").value(15.84)));

        // En borrador se puede rehacer: ahora 30 unidades sueltas, a 7,20 / 24 = 0,30
        perform(put("/api/purchase-orders/" + orderId), manager, order.formatted(supplierId, waterId, "30", ",\"unit\":\"ud\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[0].unitPrice").value(0.3))
                .andExpect(jsonPath("$.totalBase").value(9.0));

        perform(post("/api/purchase-orders/" + orderId + "/send"), manager, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"));
        perform(put("/api/purchase-orders/" + orderId), manager, order.formatted(supplierId, waterId, "1", ""))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/purchase-orders/" + orderId).header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isConflict());
        perform(post("/api/purchase-orders/" + orderId + "/cancel"), manager, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        perform(post("/api/purchase-orders/" + orderId + "/cancel"), manager, "")
                .andExpect(status().isConflict());

        // La categoría tiene artículos: no se borra
        mockMvc.perform(delete("/api/item-categories/" + categoryId).header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isConflict());
    }

    @Test
    void staffCannotCreatePurchaseOrders() throws Exception {
        perform(post("/api/purchase-orders"), staff, "{\"supplierId\":1,\"lines\":[{\"stockItemId\":1,\"quantity\":1}]}")
                .andExpect(status().isForbidden());
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String token, String body) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private long vatRateId(String name) throws Exception {
        String rates = mockMvc.perform(get("/api/vat-rates").header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(rates, "$[?(@.name == '" + name + "')].id");
        return ids.get(0);
    }

    private static long idOf(ResultActions result) throws Exception {
        Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private String bearer(UserRole role) {
        String email = role.name().toLowerCase() + "-" + System.nanoTime() + "@bar.com";
        User user = new User();
        user.setName("Usuario " + role);
        user.setEmail(email);
        user.setPassword("no-se-usa");
        user.setPhone("+34600000099");
        user.setRole(role);
        User saved = userRepository.save(user);
        return "Bearer " + tokenService.generateToken(saved.getId(), email, role);
    }
}
