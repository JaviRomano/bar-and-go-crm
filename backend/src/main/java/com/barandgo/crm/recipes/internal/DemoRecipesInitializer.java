package com.barandgo.crm.recipes.internal;

import java.math.BigDecimal;
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
import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.identity.UserResponseDTO;
import com.barandgo.crm.identity.UserService;
import com.barandgo.crm.inventory.WarehouseDTO;
import com.barandgo.crm.inventory.WarehouseService;
import com.barandgo.crm.masterdata.ItemCategoryDTO;
import com.barandgo.crm.masterdata.ItemCategoryService;
import com.barandgo.crm.masterdata.VatRateDTO;
import com.barandgo.crm.masterdata.VatRateService;
import com.barandgo.crm.purchasing.StockItemDTO;
import com.barandgo.crm.purchasing.StockItemService;
import com.barandgo.crm.recipes.RecipeDTOs.LineRequest;
import com.barandgo.crm.recipes.RecipeDTOs.ProductionRequest;
import com.barandgo.crm.recipes.RecipeDTOs.RecipeDTO;
import com.barandgo.crm.recipes.RecipeDTOs.RecipeRequest;
import com.barandgo.crm.recipes.RecipeService;
import com.barandgo.crm.recipes.RecipeType;

/**
 * Escandallos de demostración para la carta demo: una salsa casera que se guarda en stock (y una
 * producción de 1 kg), varios platos que la usan o no, y las bebidas de reventa. Solo en dev, sin
 * escandallos previos y con los artículos de compra y el inventario de demo cargados.
 */
@Component
@Profile("dev")
@Order(40)
class DemoRecipesInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DemoRecipesInitializer.class);

    private final RecipeRepository recipeRepository;
    private final RecipeService recipeService;
    private final StockItemService stockItemService;
    private final ItemCategoryService categoryService;
    private final VatRateService vatRateService;
    private final ProductService productService;
    private final WarehouseService warehouseService;
    private final UserService userService;

    private Map<String, Long> items;
    private Map<String, Long> products;
    private CurrentUser admin;

    DemoRecipesInitializer(RecipeRepository recipeRepository, RecipeService recipeService,
                           StockItemService stockItemService, ItemCategoryService categoryService,
                           VatRateService vatRateService, ProductService productService,
                           WarehouseService warehouseService, UserService userService) {
        this.recipeRepository = recipeRepository;
        this.recipeService = recipeService;
        this.stockItemService = stockItemService;
        this.categoryService = categoryService;
        this.vatRateService = vatRateService;
        this.productService = productService;
        this.warehouseService = warehouseService;
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        if (recipeRepository.count() > 0) {
            logger.info("Ya hay escandallos. Se omite la carga de demo de escandallos.");
            return;
        }
        items = stockItemService.getStockItems(null, null).stream()
                .collect(Collectors.toMap(StockItemDTO::code, StockItemDTO::id));
        if (!items.containsKey("TOMATE-TR")) {
            logger.info("No están los artículos de compra de demo. Se omite la carga de demo de escandallos.");
            return;
        }
        products = productService.getAllProducts().stream()
                .collect(Collectors.toMap(ProductDTO::getName, ProductDTO::getId, (a, b) -> a));
        UserResponseDTO adminUser = userService.getUserByEmail("admin@bar.com");
        admin = new CurrentUser(adminUser.getId(), adminUser.getEmail(), adminUser.getRole());

        Long sauceItem = sauceStockItem();
        RecipeDTO sauce = recipeService.createRecipe(new RecipeRequest("Salsa de tomate casera", RecipeType.PREPARATION,
                null, sauceItem, null, new BigDecimal("2"), "kg", null, "Para pizzas y bravas", true, List.of(
                        item("TOMATE-TR", "2.2", "kg", null),
                        item("ACEITE-OV", "100", "ml", null))), admin);

        dish("Pizza Margarita", "1", item("HARINA", "180", "g", null), sub(sauce.id(), "90", "g"),
                item("MOZZ", "125", "g", null), item("ACEITE-OV", "10", "ml", null));
        dish("Hamburguesa Clásica", "1", item("TERN-PIC", "200", "g", null), item("PATATA", "250", "g", "80"),
                item("ACEITE-OV", "30", "ml", null));
        dish("Patatas Bravas", "1", item("PATATA", "300", "g", "85"), item("ACEITE-OV", "40", "ml", null),
                sub(sauce.id(), "60", "g"));
        dish("Pollo a la Plancha", "1", item("POLLO-PEC", "250", "g", "90"), item("PATATA", "200", "g", "80"),
                item("ACEITE-OV", "20", "ml", null));
        dish("Coca-Cola", "1", item("COCA-33", "1", "ud", null));
        dish("Agua Mineral", "1", item("AGUA-50", "1", "ud", null));
        dish("Cerveza Estrella Galicia", "1", item("CERV-EG33", "1", "ud", null));

        // Se elabora 1 kg de salsa en la cámara, donde está el tomate recibido
        warehouseService.getWarehouses(false).stream()
                .filter(w -> "Cámara".equals(w.name()))
                .map(WarehouseDTO::id)
                .findFirst()
                .ifPresent(coldRoom -> recipeService.produce(sauce.id(),
                        new ProductionRequest(coldRoom, BigDecimal.ONE, null, "Producción de demostración"), admin));

        logger.info("Escandallos de demo cargados: {}", recipeRepository.count());
    }

    private Long sauceStockItem() {
        Long category = categoryService.getCategories().stream()
                .filter(c -> "Elaboraciones".equals(c.name()))
                .map(ItemCategoryDTO::id)
                .findFirst()
                .orElseGet(() -> categoryService.createCategory(new ItemCategoryDTO(null, "Elaboraciones", true)).id());
        Long vat = vatRateService.getVatRates().stream()
                .filter(rate -> rate.rate().compareTo(new BigDecimal("10.00")) == 0)
                .map(VatRateDTO::id)
                .findFirst()
                .orElseThrow();
        return stockItemService.createStockItem(new StockItemDTO(null, "SALSA-TOM", "Salsa de tomate casera", category,
                null, "kg", "kg", "g", null, null, vat, null, null, true, null, true)).id();
    }

    private void dish(String productName, String portions, LineRequest... lines) {
        Long productId = products.get(productName);
        if (productId == null) {
            return;
        }
        recipeService.createRecipe(new RecipeRequest(productName, RecipeType.DISH, productId, null,
                new BigDecimal(portions), null, null, null, null, true, new ArrayList<>(List.of(lines))), admin);
    }

    private LineRequest item(String code, String quantity, String unit, String yieldPct) {
        return new LineRequest(items.get(code), null, new BigDecimal(quantity), unit,
                yieldPct == null ? null : new BigDecimal(yieldPct));
    }

    private static LineRequest sub(Long recipeId, String quantity, String unit) {
        return new LineRequest(null, recipeId, new BigDecimal(quantity), unit, null);
    }
}
