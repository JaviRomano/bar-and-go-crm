package com.barandgo.crm.costcontrol.internal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

import com.barandgo.crm.catalog.ProductDTO;
import com.barandgo.crm.catalog.ProductService;
import com.barandgo.crm.costcontrol.CostControlDTOs.WasteRequest;
import com.barandgo.crm.costcontrol.CostControlService;
import com.barandgo.crm.costcontrol.WasteReason;
import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.identity.UserResponseDTO;
import com.barandgo.crm.identity.UserService;
import com.barandgo.crm.inventory.WarehouseDTO;
import com.barandgo.crm.inventory.WarehouseService;
import com.barandgo.crm.ordering.OrderItemRequestDTO;
import com.barandgo.crm.ordering.OrderRequestDTO;
import com.barandgo.crm.ordering.OrderResponseDTO;
import com.barandgo.crm.ordering.OrderService;
import com.barandgo.crm.ordering.OrderStatus;
import com.barandgo.crm.ordering.OrderUpdateStatusDTO;
import com.barandgo.crm.ordering.PaymentMethod;
import com.barandgo.crm.purchasing.StockItemDTO;
import com.barandgo.crm.purchasing.StockItemService;
import com.barandgo.crm.recipes.RecipeService;
import com.barandgo.crm.recipes.RecipeType;
import com.barandgo.crm.shared.BadRequestException;

/**
 * Ventas y mermas de demostración para que los informes tengan datos: cuatro pedidos entregados por el
 * personal (descuentan stock con sus escandallos) y dos mermas. Solo en dev, sin mermas ni descuentos
 * previos y con los escandallos de demo cargados.
 */
@Component
@Profile("dev")
@Order(50)
class DemoCostControlInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DemoCostControlInitializer.class);

    private final WasteRepository wasteRepository;
    private final OrderStockPostingRepository postingRepository;
    private final CostControlService costControlService;
    private final OrderService orderService;
    private final ProductService productService;
    private final RecipeService recipeService;
    private final StockItemService stockItemService;
    private final WarehouseService warehouseService;
    private final UserService userService;

    private Map<String, Long> products;

    DemoCostControlInitializer(WasteRepository wasteRepository, OrderStockPostingRepository postingRepository,
                               CostControlService costControlService, OrderService orderService,
                               ProductService productService, RecipeService recipeService,
                               StockItemService stockItemService, WarehouseService warehouseService,
                               UserService userService) {
        this.wasteRepository = wasteRepository;
        this.postingRepository = postingRepository;
        this.costControlService = costControlService;
        this.orderService = orderService;
        this.productService = productService;
        this.recipeService = recipeService;
        this.stockItemService = stockItemService;
        this.warehouseService = warehouseService;
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        if (wasteRepository.count() > 0 || postingRepository.count() > 0) {
            logger.info("Ya hay ventas o mermas registradas. Se omite la carga de demo de control de costes.");
            return;
        }
        CurrentUser staff = user("personal@bar.com");
        if (recipeService.getRecipes(RecipeType.DISH, staff).isEmpty()) {
            logger.info("No hay escandallos de demo. Se omite la carga de demo de control de costes.");
            return;
        }
        products = productService.getAllProducts().stream()
                .collect(Collectors.toMap(ProductDTO::getName, ProductDTO::getId, (a, b) -> a));

        // Mezcla pensada para que la matriz de menú tenga platos en varios cuadrantes
        deliver(staff, "ripleynostromo@example.com", "Pizza Margarita", 2, "Cerveza Estrella Galicia", 2, "Agua Mineral", 1);
        deliver(staff, "xwingluke@example.com", "Hamburguesa Clásica", 1, "Coca-Cola", 2, "Patatas Bravas", 1);
        deliver(staff, "tepersigue@example.com", "Cerveza Estrella Galicia", 3, "Pizza Margarita", 1, "Patatas Bravas", 1);
        deliver(staff, "ripleynostromo@example.com", "Pollo a la Plancha", 1, "Agua Mineral", 1);

        warehouseService.getSalesWarehouse().map(WarehouseDTO::id).ifPresent(warehouse -> {
            Map<String, Long> items = stockItemService.getStockItems(null, null).stream()
                    .collect(Collectors.toMap(StockItemDTO::code, StockItemDTO::id));
            waste(staff, new WasteRequest(warehouse, items.get("TOMATE-TR"), null, new BigDecimal("500"), "g",
                    WasteReason.EXPIRED, "Lata abierta de más de tres días"));
            waste(staff, new WasteRequest(warehouse, null, products.get("Patatas Bravas"), BigDecimal.ONE, null,
                    WasteReason.ORDER_ERROR, "Comanda duplicada"));
        });
        logger.info("Ventas y mermas de demo cargadas");
    }

    /** Pedido de un cliente demo que el personal lleva hasta entregado. Pares nombre de producto, cantidad. */
    private void deliver(CurrentUser staff, String customerEmail, Object... lines) {
        List<OrderItemRequestDTO> items = new ArrayList<>();
        for (int i = 0; i < lines.length; i += 2) {
            Long productId = products.get((String) lines[i]);
            if (productId != null) {
                OrderItemRequestDTO item = new OrderItemRequestDTO();
                item.setProductId(productId);
                item.setQuantity((Integer) lines[i + 1]);
                items.add(item);
            }
        }
        if (items.isEmpty()) {
            return;
        }
        OrderRequestDTO request = new OrderRequestDTO();
        request.setTimeTakeAway(LocalDateTime.now().plusHours(1));
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setItems(items);
        OrderResponseDTO order = orderService.createOrder(user(customerEmail).id(), request);
        for (OrderStatus status : List.of(OrderStatus.IN_PREPARATION, OrderStatus.READY, OrderStatus.DELIVERED)) {
            OrderUpdateStatusDTO update = new OrderUpdateStatusDTO();
            update.setStatus(status);
            orderService.updateOrderStatus(order.getId(), update, staff);
        }
    }

    private void waste(CurrentUser staff, WasteRequest request) {
        try {
            costControlService.registerWaste(request, staff);
        } catch (BadRequestException e) {
            // Sin stock suficiente en el almacén de ventas: la demo sigue sin esa merma
            logger.info("Merma de demo omitida: {}", e.getMessage());
        }
    }

    private CurrentUser user(String email) {
        UserResponseDTO user = userService.getUserByEmail(email);
        return new CurrentUser(user.getId(), user.getEmail(), user.getRole());
    }
}
