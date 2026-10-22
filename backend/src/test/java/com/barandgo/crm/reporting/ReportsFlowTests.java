package com.barandgo.crm.reporting;

import static com.barandgo.crm.support.ErpTestApi.body;
import static com.barandgo.crm.support.ErpTestApi.idOf;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.barandgo.crm.identity.UserRole;
import com.barandgo.crm.identity.internal.TokenService;
import com.barandgo.crm.identity.internal.UserRepository;
import com.barandgo.crm.support.ErpTestApi;
import com.barandgo.crm.support.PostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;

/**
 * Informes de principio a fin contra PostgreSQL, con importes calculables a mano: dos compras de cerveza a
 * precios distintos (PMP 1,25), tomate a 2,00, una venta de 3 botellines y una tapa, y una merma.
 */
@PostgresIntegrationTest
class ReportsFlowTests {

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
    private String customer;

    @BeforeEach
    void setUp() {
        api = new ErpTestApi(mockMvc, userRepository, tokenService);
        manager = api.bearer(UserRole.MANAGER);
        staff = api.bearer(UserRole.STAFF);
        customer = api.bearer(UserRole.CUSTOMER);
    }

    @Test
    void managementReports() throws Exception {
        long categoryId = api.itemCategory(manager, "Bebidas y cocina");
        long supplierId = api.supplier(manager, "B12345674", "Distribuciones Norte SL", 1);
        long vatId = api.vatRateId(manager, "Reducido");
        long warehouse = idOf(api.call(post("/api/warehouses"), manager, """
                {"name":"Cámara","type":"COLD_ROOM","defaultForSales":true}
                """).andExpect(status().isCreated()));
        String item = """
                {"code":"%s","name":"%s","itemCategoryId":%d,"purchaseUnit":"%s","stockUnit":"%s","usageUnit":"%s",
                 "vatRateId":%d}
                """;
        long beer = api.stockItem(manager, item.formatted("CERVEZA", "Cerveza botellín", categoryId, "ud", "ud", "ud",
                vatId));
        long tomato = api.stockItem(manager, item.formatted("TOMATE", "Tomate", categoryId, "kg", "kg", "g", vatId));

        // Cerveza: 24 a 1,00 y 24 a 1,50 -> PMP 1,25. Tomate: 10 kg a 2,00
        api.tariff(manager, beer, supplierId, "ud", "1.00");
        api.tariff(manager, tomato, supplierId, "kg", "2.00");
        receipt(supplierId, warehouse, "{\"stockItemId\":%d,\"quantity\":24},{\"stockItemId\":%d,\"quantity\":10}"
                .formatted(beer, tomato));
        api.tariff(manager, beer, supplierId, "ud", "1.50");
        receipt(supplierId, warehouse, "{\"stockItemId\":%d,\"quantity\":24}".formatted(beer));

        long productCategory = JsonPath.<Number>read(body(api.fetch("/api/product-categories", manager)), "$[0].id")
                .longValue();
        long beerProduct = product(productCategory, "Botellín", "2.75");      // 2,50 sin IVA
        long tapaProduct = product(productCategory, "Tapa de tomate", "5.50"); // 5,00 sin IVA
        recipe("Botellín", beerProduct, beer, "1", "ud");
        recipe("Tapa", tapaProduct, tomato, "250", "g");

        long order = idOf(api.call(post("/api/orders"), customer, """
                {"timeTakeAway":"%s","paymentMethod":"CASH","items":[{"productId":%d,"quantity":3},
                 {"productId":%d,"quantity":1}]}
                """.formatted(LocalDateTime.now().plusHours(2).withNano(0), beerProduct, tapaProduct))
                .andExpect(status().isCreated()));
        for (String next : List.of("IN_PREPARATION", "READY", "DELIVERED")) {
            api.call(patch("/api/orders/" + order + "/status"), staff, "{\"status\":\"" + next + "\"}")
                    .andExpect(status().isOk());
        }
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                api.fetch("/api/cost-control/orders/" + order + "/stock-posting", manager).andExpect(status().isOk()));
        api.call(post("/api/waste"), staff, """
                {"warehouseId":%d,"stockItemId":%d,"quantity":1,"reason":"BREAKAGE"}
                """.formatted(warehouse, beer)).andExpect(status().isCreated());

        // Stock: 44 botellines a 1,25 + 9,75 kg de tomate a 2,00
        api.fetch("/api/reports/stock-value", manager)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalValue").value(74.5))
                .andExpect(jsonPath("$.lines[0].stockItemCode").value("CERVEZA"))
                .andExpect(jsonPath("$.lines[0].quantity").value(44.0))
                .andExpect(jsonPath("$.lines[0].averageCost").value(1.25))
                .andExpect(jsonPath("$.byWarehouse", hasSize(1)));

        // Ventas 12,50 sin IVA. Teórico 3 x 1,25 + 0,25 x 2,00 = 4,25 (34 %). Real = teórico + merma 1,25 (44 %)
        api.fetch("/api/reports/food-cost", manager)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossRevenue").value(13.75))
                .andExpect(jsonPath("$.netRevenue").value(12.5))
                .andExpect(jsonPath("$.theoreticalCost").value(4.25))
                .andExpect(jsonPath("$.wasteCost").value(1.25))
                .andExpect(jsonPath("$.realCost").value(5.5))
                .andExpect(jsonPath("$.theoreticalPct").value(34.0))
                .andExpect(jsonPath("$.realPct").value(44.0))
                .andExpect(jsonPath("$.deviationPts").value(10.0))
                .andExpect(jsonPath("$.bucket").value("DAY"))
                .andExpect(jsonPath("$.series", hasSize(30)));

        api.fetch("/api/reports/waste", manager)
                .andExpect(jsonPath("$.totalValue").value(1.25))
                .andExpect(jsonPath("$.byReason[0].reason").value("BREAKAGE"))
                .andExpect(jsonPath("$.byReason[0].sharePct").value(100.0))
                .andExpect(jsonPath("$.byItem[0].quantity").value(1.0));

        // Todo entró dentro del periodo: stock medio (0 + 74,50) / 2, consumo 4,25 + 1,25
        api.fetch("/api/reports/rotation", manager)
                .andExpect(jsonPath("$.days").value(30))
                .andExpect(jsonPath("$.totalConsumption").value(5.5))
                .andExpect(jsonPath("$.totalAverageValue").value(37.25));

        api.fetch("/api/reports/supplier-prices?stockItemId=" + beer, manager)
                .andExpect(jsonPath("$.series", hasSize(1)))
                .andExpect(jsonPath("$.series[0].points", hasSize(2)))
                .andExpect(jsonPath("$.series[0].firstPrice").value(1.0))
                .andExpect(jsonPath("$.series[0].lastPrice").value(1.5))
                .andExpect(jsonPath("$.series[0].changePct").value(50.0));

        // Botellín: 75 % de las ventas, margen 2,50 - 1,50 = 1,00. Tapa: 25 %, margen 5,00 - 0,50 = 4,50.
        // Umbrales: popularidad 70 % de 50 % = 35 %; margen medio ponderado (3 x 1,00 + 4,50) / 4 = 1,88
        api.fetch("/api/reports/menu-engineering", manager)
                .andExpect(jsonPath("$.totalSold").value(4))
                .andExpect(jsonPath("$.popularityThresholdPct").value(35.0))
                .andExpect(jsonPath("$.marginThreshold").value(1.88))
                .andExpect(jsonPath("$.items[?(@.productName == 'Botellín')].classification").value("PLOWHORSE"))
                .andExpect(jsonPath("$.items[?(@.productName == 'Tapa de tomate')].classification").value("PUZZLE"));

        api.fetch("/api/reports/food-cost?from=2026-02-10&to=2026-02-01", manager).andExpect(status().isBadRequest());
        api.fetch("/api/reports/food-cost", staff).andExpect(status().isForbidden());
    }

    private void receipt(long supplierId, long warehouseId, String lines) throws Exception {
        api.call(post("/api/goods-receipts"), manager, """
                {"supplierId":%d,"warehouseId":%d,"lines":[%s]}
                """.formatted(supplierId, warehouseId, lines)).andExpect(status().isCreated());
    }

    private long product(long categoryId, String name, String price) throws Exception {
        return idOf(api.call(post("/api/products"), manager, """
                {"name":"%s","price":%s,"categoryId":%d}
                """.formatted(name, price, categoryId)).andExpect(status().isCreated()));
    }

    private void recipe(String name, long productId, long stockItemId, String quantity, String unit) throws Exception {
        api.call(post("/api/recipes"), manager, """
                {"name":"%s","type":"DISH","productId":%d,"portions":1,
                 "lines":[{"stockItemId":%d,"grossQuantity":%s,"unit":"%s"}]}
                """.formatted(name, productId, stockItemId, quantity, unit)).andExpect(status().isCreated());
    }
}
