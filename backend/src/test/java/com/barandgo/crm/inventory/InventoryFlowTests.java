package com.barandgo.crm.inventory;

import static com.barandgo.crm.support.ErpTestApi.body;
import static com.barandgo.crm.support.ErpTestApi.idOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
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
 * Inventario de principio a fin contra PostgreSQL: recepción contra pedido, PMP, lotes, traspasos,
 * ajustes, recuento, reposición e inmutabilidad del ledger. Se omite sin Docker.
 */
@PostgresIntegrationTest
class InventoryFlowTests {

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
    private JdbcTemplate jdbcTemplate;

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
    void stockLifecycleFromReceiptToReplenishment() throws Exception {
        long categoryId = api.itemCategory(manager, "Bebidas y lácteos");
        long supplierId = api.supplier(manager, "A58818501", "Distribuciones Norte SA", 1);
        long vatId = api.vatRateId(manager, "Reducido");
        long coldRoom = api.warehouse(manager, "Cámara", "COLD_ROOM");
        long bar = api.warehouse(manager, "Barra", "BAR");

        long water = api.stockItem(manager, """
                {"code":"AGUA-50","name":"Agua 50 cl","itemCategoryId":%d,"purchaseUnit":"caja","stockUnit":"ud",
                 "usageUnit":"ud","vatRateId":%d,"minStock":48,"maxStock":96,"preferredSupplierId":%d,
                 "conversions":[{"fromUnit":"caja","toUnit":"ud","factor":24}]}
                """.formatted(categoryId, vatId, supplierId));
        long milk = api.stockItem(manager, """
                {"code":"LECHE","name":"Leche entera","itemCategoryId":%d,"purchaseUnit":"caja","stockUnit":"l",
                 "usageUnit":"ml","vatRateId":%d,"perishable":true,"allergens":["MILK"],
                 "conversions":[{"fromUnit":"caja","toUnit":"l","factor":6}]}
                """.formatted(categoryId, vatId));
        api.tariff(manager, water, supplierId, "caja", "7.20");
        api.tariff(manager, milk, supplierId, "caja", "6.00");

        long orderId = api.sentPurchaseOrder(manager, supplierId,
                "[{\"stockItemId\":%d,\"quantity\":2},{\"stockItemId\":%d,\"quantity\":1}]".formatted(water, milk));

        // 1. Recepción parcial: 1 caja de agua (24 ud a 0,30) y la leche con su lote
        String expiry = LocalDate.now().plusDays(2).toString();
        String firstReceipt = """
                {"supplierId":%d,"purchaseOrderId":%d,"warehouseId":%d,"supplierDeliveryNote":"DN-1",
                 "lines":[{"stockItemId":%d,"quantity":1},
                          {"stockItemId":%d,"quantity":1,"lotCode":"L-0042","expiryDate":"%s"}]}
                """.formatted(supplierId, orderId, coldRoom, water, milk, expiry);
        api.call(post("/api/goods-receipts"), staff, firstReceipt)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value(startsWith("AL-")))
                .andExpect(jsonPath("$.lines[0].stockQuantity").value(24))
                .andExpect(jsonPath("$.lines[0].stockUnitCost").value(0.3))
                .andExpect(jsonPath("$.lines[1].lotCode").value("L-0042"));
        api.fetch("/api/purchase-orders/" + orderId, manager)
                .andExpect(jsonPath("$.status").value("PARTIALLY_RECEIVED"))
                .andExpect(jsonPath("$.lines[0].receivedQuantity").value(1));

        // Perecedero sin lote: rechazado
        api.call(post("/api/goods-receipts"), manager, """
                {"supplierId":%d,"warehouseId":%d,"lines":[{"stockItemId":%d,"quantity":1}]}
                """.formatted(supplierId, coldRoom, milk)).andExpect(status().isBadRequest());

        // El personal no puede fijar un precio distinto del pedido
        String secondReceipt = """
                {"supplierId":%d,"purchaseOrderId":%d,"warehouseId":%d,
                 "lines":[{"stockItemId":%d,"quantity":1,"unitPrice":9.60}]}
                """.formatted(supplierId, orderId, coldRoom, water);
        api.call(post("/api/goods-receipts"), staff, secondReceipt).andExpect(status().isForbidden());

        // 2. El encargado sí: 24 ud a 0,40. PMP = (24 x 0,30 + 24 x 0,40) / 48 = 0,35
        api.call(post("/api/goods-receipts"), manager, secondReceipt).andExpect(status().isCreated());
        api.fetch("/api/purchase-orders/" + orderId, manager).andExpect(jsonPath("$.status").value("RECEIVED"));
        api.fetch("/api/inventory/valuation", manager)
                .andExpect(jsonPath("$.lines[?(@.stockItemCode == 'AGUA-50')].averageCost").value(0.35))
                .andExpect(jsonPath("$.lines[?(@.stockItemCode == 'AGUA-50')].value").value(16.8));
        // El último precio pagado pasa a la tarifa y queda en el histórico
        api.fetch("/api/stock-items/" + water + "/suppliers", manager)
                .andExpect(jsonPath("$[0].unitPrice").value(9.6));
        api.fetch("/api/stock-items/" + water + "/price-history", manager).andExpect(jsonPath("$", hasSize(2)));

        // 3. Traspasos: el PMP no cambia y los lotes viajan con la mercancía
        String transfer = "{\"fromWarehouseId\":%d,\"toWarehouseId\":%d,\"stockItemId\":%d,\"quantity\":%s}";
        api.call(post("/api/inventory/transfers"), staff, transfer.formatted(coldRoom, bar, water, "12"))
                .andExpect(status().isCreated());
        api.call(post("/api/inventory/transfers"), staff, transfer.formatted(coldRoom, bar, milk, "2"))
                .andExpect(status().isCreated());
        api.call(post("/api/inventory/transfers"), staff, transfer.formatted(coldRoom, bar, water, "100"))
                .andExpect(status().isBadRequest());
        assertStock(water, coldRoom, 36);
        assertStock(water, bar, 12);
        api.fetch("/api/inventory/valuation", manager)
                .andExpect(jsonPath("$.lines[?(@.stockItemCode == 'AGUA-50')].averageCost").value(0.35));
        api.fetch("/api/inventory/lots/expiring?days=7", staff)
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.warehouseId == %d)].remainingQuantity".formatted(coldRoom)).value(4.0))
                .andExpect(jsonPath("$[?(@.warehouseId == %d)].remainingQuantity".formatted(bar)).value(2.0));

        // 4. Ajuste manual (rotura de una botella en barra)
        api.call(post("/api/inventory/adjustments"), manager, """
                {"warehouseId":%d,"stockItemId":%d,"quantity":-1,"reason":"Rotura"}
                """.formatted(bar, water)).andExpect(status().isCreated());
        assertStock(water, bar, 11);

        // 5. Recuento en cámara: se cuentan 35 ud de agua donde el teórico es 36
        long countId = idOf(api.call(post("/api/stock-counts"), manager, "{\"warehouseId\":%d}".formatted(coldRoom))
                .andExpect(status().isCreated()));
        api.call(put("/api/stock-counts/" + countId + "/lines"), staff,
                        "{\"entries\":[{\"stockItemId\":%d,\"countedQuantity\":35}]}".formatted(water))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.lines[?(@.stockItemId == %d)].theoreticalQuantity".formatted(water))
                        .value(contains(nullValue())));
        api.call(post("/api/stock-counts/" + countId + "/close"), manager, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.lines[?(@.stockItemId == %d)].difference".formatted(water)).value(-1.0))
                .andExpect(jsonPath("$.totalDifferenceValue").value(-0.35));
        assertStock(water, coldRoom, 35);

        // 6. Reposición: 35 + 11 = 46 < 48 -> hasta 96 son 50 ud -> 3 cajas
        String suggestions = body(api.fetch("/api/inventory/replenishment", manager).andExpect(status().isOk()));
        List<Number> boxes = JsonPath.read(suggestions,
                "$[?(@.supplierId == %d)].lines[?(@.stockItemId == %d)].suggestedPurchaseQuantity"
                        .formatted(supplierId, water));
        assertThat(boxes).singleElement().satisfies(n -> assertThat(n.intValue()).isEqualTo(3));
        api.call(post("/api/inventory/replenishment/orders"), manager, "{}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[0].lines[0].quantity").value(3))
                .andExpect(jsonPath("$[0].lines[0].unitPrice").value(9.6));

        // 7. Con movimientos, el artículo ya no puede cambiar de unidad de inventario
        api.call(put("/api/stock-items/" + water), manager, """
                {"code":"AGUA-50","name":"Agua 50 cl","itemCategoryId":%d,"purchaseUnit":"caja","stockUnit":"caja",
                 "usageUnit":"caja","vatRateId":%d}
                """.formatted(categoryId, vatId)).andExpect(status().isConflict());

        // 8. El ledger no se puede modificar ni desde SQL
        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE stock_movement SET quantity = 999"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("inmutable");
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM stock_movement"))
                .isInstanceOf(DataAccessException.class);
    }

    private void assertStock(long itemId, long warehouseId, int expected) throws Exception {
        api.fetch("/api/inventory/stock?stockItemId=%d&warehouseId=%d".formatted(itemId, warehouseId), staff)
                .andExpect(jsonPath("$[0].quantity").value(expected));
    }
}
