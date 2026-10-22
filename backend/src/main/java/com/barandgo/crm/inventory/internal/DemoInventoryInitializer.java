package com.barandgo.crm.inventory.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.identity.UserResponseDTO;
import com.barandgo.crm.identity.UserService;
import com.barandgo.crm.inventory.GoodsReceiptDTOs.GoodsReceiptRequestDTO;
import com.barandgo.crm.inventory.GoodsReceiptDTOs.LineRequest;
import com.barandgo.crm.inventory.GoodsReceiptService;
import com.barandgo.crm.inventory.InventoryDTOs.TransferRequestDTO;
import com.barandgo.crm.inventory.InventoryService;
import com.barandgo.crm.inventory.WarehouseDTO;
import com.barandgo.crm.inventory.WarehouseService;
import com.barandgo.crm.inventory.WarehouseType;
import com.barandgo.crm.masterdata.SupplierDTO;
import com.barandgo.crm.masterdata.SupplierService;
import com.barandgo.crm.purchasing.PurchaseOrderDTO;
import com.barandgo.crm.purchasing.PurchaseOrderLineRequestDTO;
import com.barandgo.crm.purchasing.PurchaseOrderRequestDTO;
import com.barandgo.crm.purchasing.PurchaseOrderService;
import com.barandgo.crm.purchasing.StockItemDTO;
import com.barandgo.crm.purchasing.StockItemService;

/**
 * Stock de demostración: almacenes y tres pedidos recibidos (uno en parte), con lotes que caducan
 * pronto y algún artículo bajo mínimo, para que stock, PMP, caducidades y reposición muestren datos.
 * Todo pasa por los servicios, como lo haría una persona. Solo en dev, sin almacenes previos y con
 * los artículos de compra de demo cargados.
 */
@Component
@Profile("dev")
@Order(30)
class DemoInventoryInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DemoInventoryInitializer.class);

    private final WarehouseService warehouseService;
    private final PurchaseOrderService purchaseOrderService;
    private final GoodsReceiptService goodsReceiptService;
    private final InventoryService inventoryService;
    private final StockItemService stockItemService;
    private final SupplierService supplierService;
    private final UserService userService;

    private Map<String, Long> items;
    private Map<String, Long> suppliers;
    private CurrentUser admin;

    DemoInventoryInitializer(WarehouseService warehouseService, PurchaseOrderService purchaseOrderService,
                             GoodsReceiptService goodsReceiptService, InventoryService inventoryService,
                             StockItemService stockItemService, SupplierService supplierService,
                             UserService userService) {
        this.warehouseService = warehouseService;
        this.purchaseOrderService = purchaseOrderService;
        this.goodsReceiptService = goodsReceiptService;
        this.inventoryService = inventoryService;
        this.stockItemService = stockItemService;
        this.supplierService = supplierService;
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        if (!warehouseService.getWarehouses(true).isEmpty()) {
            logger.info("Ya hay almacenes. Se omite la carga de demo de inventario.");
            return;
        }
        items = stockItemService.getStockItems(null, null).stream()
                .collect(Collectors.toMap(StockItemDTO::code, StockItemDTO::id));
        suppliers = supplierService.getSuppliers(null).stream()
                .collect(Collectors.toMap(SupplierDTO::legalName, SupplierDTO::id));
        if (!items.containsKey("AGUA-50")) {
            logger.info("No están los artículos de compra de demo. Se omite la carga de demo de inventario.");
            return;
        }
        UserResponseDTO adminUser = userService.getUserByEmail("admin@bar.com");
        admin = new CurrentUser(adminUser.getId(), adminUser.getEmail(), adminUser.getRole());

        long coldRoom = warehouse("Cámara", WarehouseType.COLD_ROOM);
        long dryStore = warehouse("Almacén seco", WarehouseType.DRY_STORE);
        long bar = warehouse("Barra", WarehouseType.BAR);
        warehouse("Congelador", WarehouseType.FREEZER);
        LocalDate today = LocalDate.now();

        // Bebidas: todo recibido en el almacén seco y una parte subida a barra
        receiveAll("Bebidas del Norte SL", dryStore, List.of(
                line("AGUA-50", "4"), line("COCA-33", "3"), line("CERV-EG33", "4")), Map.of());
        transfer(dryStore, bar, "AGUA-50", "24");
        transfer(dryStore, bar, "CERV-EG33", "36");

        // Carne a la cámara, con lotes; la ternera caduca en dos días
        receiveAll("Cárnicas Sierra SA", coldRoom, List.of(
                line("TERN-PIC", "6"), line("POLLO-PEC", "5"), line("BACON", "2")), Map.of(
                "TERN-PIC", new Lot("T-" + today, today.plusDays(2)),
                "POLLO-PEC", new Lot("P-" + today, today.plusDays(4)),
                "BACON", new Lot("B-" + today, today.plusDays(20))));

        // Ultramarinos: de la leche solo llegan 2 de 3 cajas (pedido recibido en parte, queda 1 pendiente)
        PurchaseOrderDTO groceries = sentOrder("Frutas y Ultramarinos García SL", List.of(
                line("LECHE", "3"), line("HUEVOS", "4"), line("MOZZ", "3"), line("HARINA", "10"),
                line("PATATA", "20"), line("ACEITE-OV", "10"), line("TOMATE-TR", "2")));
        Map<String, Lot> groceryLots = Map.of(
                "LECHE", new Lot("L-" + today, today.plusDays(6)),
                "HUEVOS", new Lot("H-" + today, today.plusDays(15)),
                "MOZZ", new Lot("M-" + today, today.plusDays(1)));
        List<LineRequest> received = new ArrayList<>();
        for (PurchaseOrderDTO.Line line : groceries.lines()) {
            BigDecimal quantity = "LECHE".equals(line.stockItemCode()) ? new BigDecimal("2") : line.quantity();
            Lot lot = groceryLots.get(line.stockItemCode());
            received.add(new LineRequest(line.stockItemId(), quantity, null, null,
                    lot != null ? lot.code() : null, lot != null ? lot.expiry() : null));
        }
        goodsReceiptService.registerReceipt(new GoodsReceiptRequestDTO(groceries.supplierId(), groceries.id(),
                coldRoom, "ALB-" + groceries.number(), null, null, received), admin);

        logger.info("Inventario de demo cargado: 4 almacenes, 3 pedidos recibidos y stock con lotes");
    }

    private record Lot(String code, LocalDate expiry) {
    }

    private long warehouse(String name, WarehouseType type) {
        return warehouseService.createWarehouse(new WarehouseDTO(null, null, name, type, true)).id();
    }

    private PurchaseOrderLineRequestDTO line(String code, String quantity) {
        return new PurchaseOrderLineRequestDTO(items.get(code), new BigDecimal(quantity), null, null);
    }

    private PurchaseOrderDTO sentOrder(String supplier, List<PurchaseOrderLineRequestDTO> lines) {
        PurchaseOrderDTO order = purchaseOrderService.createPurchaseOrder(
                new PurchaseOrderRequestDTO(suppliers.get(supplier), null, "Pedido de demostración", lines), admin);
        return purchaseOrderService.sendPurchaseOrder(order.id());
    }

    private void receiveAll(String supplier, long warehouseId, List<PurchaseOrderLineRequestDTO> lines,
                            Map<String, Lot> lots) {
        PurchaseOrderDTO order = sentOrder(supplier, lines);
        List<LineRequest> received = order.lines().stream()
                .map(line -> {
                    Lot lot = lots.get(line.stockItemCode());
                    return new LineRequest(line.stockItemId(), line.quantity(), null, null,
                            lot != null ? lot.code() : null, lot != null ? lot.expiry() : null);
                })
                .toList();
        goodsReceiptService.registerReceipt(new GoodsReceiptRequestDTO(order.supplierId(), order.id(), warehouseId,
                "ALB-" + order.number(), null, null, received), admin);
    }

    private void transfer(long from, long to, String code, String quantity) {
        inventoryService.transfer(new TransferRequestDTO(from, to, items.get(code), new BigDecimal(quantity), null,
                "Reposición de barra"), admin);
    }
}
