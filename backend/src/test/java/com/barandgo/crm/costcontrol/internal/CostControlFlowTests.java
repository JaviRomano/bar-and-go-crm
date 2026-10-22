package com.barandgo.crm.costcontrol.internal;

import static com.barandgo.crm.support.ErpTestApi.body;
import static com.barandgo.crm.support.ErpTestApi.idOf;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
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
import com.barandgo.crm.ordering.OrderEvents.OrderDelivered;
import com.barandgo.crm.support.ErpTestApi;
import com.barandgo.crm.support.PostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;

/**
 * Control de costes de principio a fin contra PostgreSQL: descuento de stock al entregar un pedido,
 * idempotencia, anulación al cancelarlo, mermas y consumo teórico frente a real entre dos recuentos.
 * Está en el paquete interno para reenviar un evento ya procesado. Se omite sin Docker.
 */
@PostgresIntegrationTest
class CostControlFlowTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private CostControlServiceImpl costControl;

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
    void salesWasteAndConsumptionAgainstCounts() throws Exception {
        long categoryId = api.itemCategory(manager, "Bebidas y cocina");
        long supplierId = api.supplier(manager, "B12345674", "Distribuciones Norte SL", 1);
        long vatId = api.vatRateId(manager, "Reducido");
        long salesWarehouse = idOf(api.call(post("/api/warehouses"), manager, """
                {"name":"Cámara","type":"COLD_ROOM","defaultForSales":true}
                """).andExpect(status().isCreated()));

        String item = """
                {"code":"%s","name":"%s","itemCategoryId":%d,"purchaseUnit":"%s","stockUnit":"%s","usageUnit":"%s",
                 "vatRateId":%d}
                """;
        long beer = api.stockItem(manager, item.formatted("CERVEZA", "Cerveza botellín", categoryId, "ud", "ud", "ud",
                vatId));
        long tomato = api.stockItem(manager, item.formatted("TOMATE", "Tomate", categoryId, "kg", "kg", "g", vatId));
        api.tariff(manager, beer, supplierId, "ud", "1.00");
        api.tariff(manager, tomato, supplierId, "kg", "2.00");
        api.call(post("/api/goods-receipts"), manager, """
                {"supplierId":%d,"warehouseId":%d,"lines":[{"stockItemId":%d,"quantity":24},{"stockItemId":%d,"quantity":10}]}
                """.formatted(supplierId, salesWarehouse, beer, tomato)).andExpect(status().isCreated());

        long productCategory = JsonPath.<Number>read(body(api.fetch("/api/product-categories", manager)), "$[0].id")
                .longValue();
        long beerProduct = product(productCategory, "Botellín", "2.50");
        long saladProduct = product(productCategory, "Ensalada de tomate", "7.00");
        long waterProduct = product(productCategory, "Agua", "1.50");
        api.call(post("/api/recipes"), manager, """
                {"name":"Botellín","type":"DISH","productId":%d,"portions":1,
                 "lines":[{"stockItemId":%d,"grossQuantity":1,"unit":"ud"}]}
                """.formatted(beerProduct, beer)).andExpect(status().isCreated());
        api.call(post("/api/recipes"), manager, """
                {"name":"Ensalada","type":"DISH","productId":%d,"portions":1,
                 "lines":[{"stockItemId":%d,"grossQuantity":300,"unit":"g"}]}
                """.formatted(saladProduct, tomato)).andExpect(status().isCreated());

        long openingCount = closedCount(salesWarehouse, beer, "24", tomato, "10");

        // Pedido entregado: 3 botellines y 2 ensaladas salen del almacén de ventas; el agua no tiene escandallo
        long order = order(beerProduct, 3, saladProduct, 2, waterProduct, 1);
        deliver(order);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                api.fetch("/api/cost-control/orders/" + order + "/stock-posting", manager)
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.totalCost").value(4.2))    // 3 x 1,00 + 0,6 kg x 2,00
                        .andExpect(jsonPath("$.productsWithoutRecipe").value(1)));
        api.fetch("/api/inventory/stock?stockItemId=" + beer, staff).andExpect(jsonPath("$[0].quantity").value(21.0));
        api.fetch("/api/inventory/stock?stockItemId=" + tomato, staff).andExpect(jsonPath("$[0].quantity").value(9.4));

        // El registro de eventos puede reenviar una entrega ya procesada: no descuenta dos veces
        costControl.postDeliveredOrder(new OrderDelivered(order, 1L, Instant.now(),
                List.of(new OrderDelivered.Line(beerProduct, 3))));
        api.fetch("/api/inventory/stock?stockItemId=" + beer, staff).andExpect(jsonPath("$[0].quantity").value(21.0));

        // Pedido entregado y cancelado después: solo el encargado puede, y el stock vuelve
        long cancelled = order(beerProduct, 1, null, 0, null, 0);
        deliver(cancelled);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                api.fetch("/api/inventory/stock?stockItemId=" + beer, staff).andExpect(jsonPath("$[0].quantity").value(20.0)));
        api.call(patch("/api/orders/" + cancelled + "/status"), staff, "{\"status\":\"CANCELLED\"}")
                .andExpect(status().isForbidden());
        api.call(patch("/api/orders/" + cancelled + "/status"), manager, "{\"status\":\"CANCELLED\"}")
                .andExpect(status().isOk());
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                api.fetch("/api/cost-control/orders/" + cancelled + "/stock-posting", manager)
                        .andExpect(jsonPath("$.reversedAt", notNullValue())));
        api.fetch("/api/inventory/stock?stockItemId=" + beer, staff).andExpect(jsonPath("$[0].quantity").value(21.0));

        // Mermas: por artículo en otra unidad y por plato (se descompone con su escandallo)
        api.call(post("/api/waste"), staff, """
                {"warehouseId":%d,"stockItemId":%d,"quantity":500,"unit":"g","reason":"EXPIRED"}
                """.formatted(salesWarehouse, tomato))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valuedCost").value(1.0))
                .andExpect(jsonPath("$.unit").value("g"));
        api.call(post("/api/waste"), staff, """
                {"warehouseId":%d,"productId":%d,"quantity":1,"reason":"BREAKAGE","notes":"Se cayó en sala"}
                """.formatted(salesWarehouse, beerProduct))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productName").value("Botellín"));
        api.call(post("/api/waste"), staff, """
                {"warehouseId":%d,"stockItemId":%d,"quantity":100,"reason":"BREAKAGE"}
                """.formatted(salesWarehouse, beer))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Stock insuficiente")));
        api.call(post("/api/waste"), staff, """
                {"warehouseId":%d,"productId":%d,"quantity":1,"reason":"OTHER"}
                """.formatted(salesWarehouse, waterProduct))
                .andExpect(status().isBadRequest());
        api.fetch("/api/waste?reason=EXPIRED", manager).andExpect(jsonPath("$", hasSize(1)));

        // Recuento final: faltan 2 botellines que nadie registró; el tomate cuadra con ventas y merma
        long closingCount = closedCount(salesWarehouse, beer, "18", tomato, "8.9");
        api.fetch("/api/cost-control/consumption?fromCountId=" + openingCount + "&toCountId=" + closingCount, manager)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines", hasSize(2)))
                .andExpect(jsonPath("$.lines[0].stockItemCode").value("CERVEZA"))
                .andExpect(jsonPath("$.lines[0].realConsumption").value(6.0))
                .andExpect(jsonPath("$.lines[0].theoreticalConsumption").value(3.0))
                .andExpect(jsonPath("$.lines[0].recordedWaste").value(1.0))
                .andExpect(jsonPath("$.lines[0].unexplained").value(2.0))
                .andExpect(jsonPath("$.lines[1].realConsumption").value(1.1))
                .andExpect(jsonPath("$.lines[1].unexplained").value(0.0))
                .andExpect(jsonPath("$.totalUnexplainedValue").value(2.0));
    }

    private long product(long categoryId, String name, String price) throws Exception {
        return idOf(api.call(post("/api/products"), manager, """
                {"name":"%s","price":%s,"categoryId":%d}
                """.formatted(name, price, categoryId)).andExpect(status().isCreated()));
    }

    /** Hasta tres líneas; las de producto null se omiten. */
    private long order(Long p1, int q1, Long p2, int q2, Long p3, int q3) throws Exception {
        StringBuilder items = new StringBuilder();
        for (Object[] line : new Object[][] {{p1, q1}, {p2, q2}, {p3, q3}}) {
            if (line[0] != null) {
                items.append(items.isEmpty() ? "" : ",")
                        .append("{\"productId\":%d,\"quantity\":%d}".formatted(line[0], line[1]));
            }
        }
        return idOf(api.call(post("/api/orders"), customer, """
                {"timeTakeAway":"%s","paymentMethod":"CASH","items":[%s]}
                """.formatted(LocalDateTime.now().plusHours(2).withNano(0), items)).andExpect(status().isCreated()));
    }

    private void deliver(long orderId) throws Exception {
        for (String status : List.of("IN_PREPARATION", "READY", "DELIVERED")) {
            api.call(patch("/api/orders/" + orderId + "/status"), staff, "{\"status\":\"" + status + "\"}")
                    .andExpect(status().isOk());
        }
    }

    private long closedCount(long warehouseId, long item1, String qty1, long item2, String qty2) throws Exception {
        long countId = idOf(api.call(post("/api/stock-counts"), manager, "{\"warehouseId\":%d}".formatted(warehouseId))
                .andExpect(status().isCreated()));
        api.call(put("/api/stock-counts/" + countId + "/lines"), staff, """
                {"entries":[{"stockItemId":%d,"countedQuantity":%s},{"stockItemId":%d,"countedQuantity":%s}]}
                """.formatted(item1, qty1, item2, qty2)).andExpect(status().isOk());
        api.call(post("/api/stock-counts/" + countId + "/close"), manager, null).andExpect(status().isOk());
        return countId;
    }
}
