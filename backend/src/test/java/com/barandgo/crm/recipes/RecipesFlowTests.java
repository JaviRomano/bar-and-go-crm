package com.barandgo.crm.recipes;

import static com.barandgo.crm.support.ErpTestApi.body;
import static com.barandgo.crm.support.ErpTestApi.idOf;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.barandgo.crm.identity.UserRole;
import com.barandgo.crm.identity.internal.TokenService;
import com.barandgo.crm.identity.internal.UserRepository;
import com.barandgo.crm.support.ErpTestApi;
import com.jayway.jsonpath.JsonPath;

/**
 * Escandallos de principio a fin contra PostgreSQL: coste con sub-escandallo y mermas, food cost,
 * alérgenos en la carta, ciclos, producción con stock real y recálculo al cambiar un precio de compra.
 * Se omite sin Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class RecipesFlowTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenService tokenService;

    private ErpTestApi api;
    private String manager;
    private String staff;

    @BeforeEach
    void setUp() {
        api = new ErpTestApi(mockMvc, userRepository, tokenService);
        manager = api.bearer(UserRole.MANAGER);
        staff = api.bearer(UserRole.STAFF);
    }

    @Test
    void recipeCostingProductionAndRecalculation() throws Exception {
        long categoryId = api.itemCategory(manager, "Cocina");
        long supplierId = api.supplier(manager, "B12345674", "Ultramarinos Sur SL", 1);
        long vatId = api.vatRateId(manager, "Reducido");
        long kitchen = api.warehouse(manager, "Cocina", "KITCHEN");

        String item = """
                {"code":"%s","name":"%s","itemCategoryId":%d,"purchaseUnit":"%s","stockUnit":"%s","usageUnit":"%s",
                 "vatRateId":%d,"allergens":%s}
                """;
        long tomato = api.stockItem(manager, item.formatted("TOMATE", "Tomate triturado", categoryId, "kg", "kg", "g",
                vatId, "[\"CELERY\"]"));
        long oil = api.stockItem(manager, item.formatted("ACEITE", "Aceite de oliva", categoryId, "l", "l", "ml", vatId, "[]"));
        long beef = api.stockItem(manager, item.formatted("TERNERA", "Ternera picada", categoryId, "kg", "kg", "g", vatId, "[]"));
        long potato = api.stockItem(manager, item.formatted("PATATA", "Patata", categoryId, "kg", "kg", "g", vatId, "[]"));
        long sauceItem = api.stockItem(manager, item.formatted("SALSA", "Salsa de tomate casera", categoryId, "kg", "kg", "g",
                vatId, "[]"));
        api.tariff(manager, tomato, supplierId, "kg", "2.00");
        api.tariff(manager, oil, supplierId, "l", "8.00");
        api.tariff(manager, beef, supplierId, "kg", "10.00");
        api.tariff(manager, potato, supplierId, "kg", "1.00");

        long productCategory = JsonPath.<Number>read(body(api.fetch("/api/product-categories", manager)), "$[0].id")
                .longValue();
        long burgerProduct = idOf(api.call(post("/api/products"), manager, """
                {"name":"Hamburguesa de la casa","price":11.00,"categoryId":%d}
                """.formatted(productCategory)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.vatRate").value(10.0)));

        // Salsa: 2,2 kg de tomate (4,40) + 100 ml de aceite (0,80) rinden 2 kg -> 2,60 €/kg
        long sauce = idOf(api.call(post("/api/recipes"), manager, """
                {"name":"Salsa de tomate","type":"PREPARATION","stockItemId":%d,"yieldQuantity":2,"yieldUnit":"kg",
                 "lines":[{"stockItemId":%d,"grossQuantity":2.2,"unit":"kg"},
                          {"stockItemId":%d,"grossQuantity":100,"unit":"ml"}]}
                """.formatted(sauceItem, tomato, oil)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.costPerYieldUnit").value(2.6)));

        // Hamburguesa: 200 g de ternera (2,00) + 250 g de patata al 80 % (0,25) + 100 g de salsa (0,26) = 2,51
        long burger = idOf(api.call(post("/api/recipes"), manager, """
                {"name":"Hamburguesa","type":"DISH","productId":%d,"portions":1,
                 "lines":[{"stockItemId":%d,"grossQuantity":200,"unit":"g"},
                          {"stockItemId":%d,"grossQuantity":250,"unit":"g","yieldPct":80},
                          {"subRecipeId":%d,"grossQuantity":100,"unit":"g"}]}
                """.formatted(burgerProduct, beef, potato, sauce)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.costPerPortion").value(2.51))
                .andExpect(jsonPath("$.lines[1].netQuantity").value(200.0))
                .andExpect(jsonPath("$.foodCostPct").value(25.1))      // sobre 10,00 sin IVA
                .andExpect(jsonPath("$.grossMargin").value(7.49))
                .andExpect(jsonPath("$.suggestedPrice").value(9.2))    // 2,51 / 30 % + 10 % IVA
                .andExpect(jsonPath("$.allergens", hasItem("CELERY"))));

        // Los alérgenos llegan a la carta pública a través del escandallo
        mockMvc.perform(get("/api/products/" + burgerProduct))
                .andExpect(jsonPath("$.allergens", hasItem("CELERY")));

        // El personal ve el escandallo, pero no sus costes
        api.fetch("/api/recipes/" + burger, staff)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costPerPortion").doesNotExist())
                .andExpect(jsonPath("$.lines[0].lineCost").doesNotExist());

        // Ciclo: una base que usa la salsa no puede pasar a ser ingrediente de la salsa
        long base = idOf(api.call(post("/api/recipes"), manager, """
                {"name":"Base de pizza","type":"PREPARATION","yieldQuantity":1,"yieldUnit":"kg",
                 "lines":[{"subRecipeId":%d,"grossQuantity":500,"unit":"g"}]}
                """.formatted(sauce)).andExpect(status().isCreated()));
        api.call(put("/api/recipes/" + sauce), manager, """
                {"name":"Salsa de tomate","type":"PREPARATION","stockItemId":%d,"yieldQuantity":2,"yieldUnit":"kg",
                 "lines":[{"stockItemId":%d,"grossQuantity":2.2,"unit":"kg"},{"subRecipeId":%d,"grossQuantity":1,"unit":"kg"}]}
                """.formatted(sauceItem, tomato, base))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("ciclo")));

        // Producción: con stock recibido a tarifa, 1 kg de salsa consume 1,1 kg de tomate y 50 ml de aceite
        api.call(post("/api/goods-receipts"), manager, """
                {"supplierId":%d,"warehouseId":%d,"lines":[{"stockItemId":%d,"quantity":5},{"stockItemId":%d,"quantity":1}]}
                """.formatted(supplierId, kitchen, tomato, oil)).andExpect(status().isCreated());
        api.call(post("/api/recipes/" + sauce + "/production"), staff, """
                {"warehouseId":%d,"quantity":1}
                """.formatted(kitchen))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalCost").value(2.6))    // 1,1 x 2,00 + 0,05 x 8,00 al PMP
                .andExpect(jsonPath("$.movements", hasSize(3)));
        api.fetch("/api/inventory/stock?stockItemId=" + sauceItem, staff).andExpect(jsonPath("$[0].quantity").value(1.0));
        api.fetch("/api/inventory/stock?stockItemId=" + tomato, staff).andExpect(jsonPath("$[0].quantity").value(3.9));

        // Sube el tomate: compras avisa con un evento y escandallos recalcula (asíncrono)
        api.tariff(manager, tomato, supplierId, "kg", "3.00");
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                api.fetch("/api/recipes/" + burger + "/cost-history", manager)
                        .andExpect(jsonPath("$", hasSize(2)))
                        .andExpect(jsonPath("$[0].reason").value("PRICE_CHANGE"))
                        .andExpect(jsonPath("$[0].costPerUnit").value(2.62)));  // salsa a 3,70 €/kg
    }
}
