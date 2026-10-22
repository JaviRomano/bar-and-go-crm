package com.barandgo.crm.purchasing.internal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.barandgo.crm.masterdata.Allergen;
import com.barandgo.crm.masterdata.ItemCategoryDTO;
import com.barandgo.crm.masterdata.ItemCategoryService;
import com.barandgo.crm.masterdata.SupplierDTO;
import com.barandgo.crm.masterdata.SupplierService;
import com.barandgo.crm.masterdata.VatRateDTO;
import com.barandgo.crm.masterdata.VatRateService;
import com.barandgo.crm.purchasing.ItemUnitConversionDTO;
import com.barandgo.crm.purchasing.StockItemDTO;
import com.barandgo.crm.purchasing.StockItemService;
import com.barandgo.crm.purchasing.StockItemSupplierDTO;

/**
 * Artículos de compra de demostración con su tarifa, pensados para la carta demo (hamburguesas,
 * pizzas, bebidas...). Solo en el perfil dev, si no hay artículos y después de los datos maestros.
 */
@Component
@Profile("dev")
@Order(20)
class DemoPurchasingInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DemoPurchasingInitializer.class);

    private final StockItemService stockItemService;
    private final ItemCategoryService categoryService;
    private final SupplierService supplierService;
    private final VatRateService vatRateService;

    private Map<String, Long> categories;
    private Map<String, Long> suppliers;
    private Map<String, Long> vatRates;

    DemoPurchasingInitializer(StockItemService stockItemService, ItemCategoryService categoryService,
                              SupplierService supplierService, VatRateService vatRateService) {
        this.stockItemService = stockItemService;
        this.categoryService = categoryService;
        this.supplierService = supplierService;
        this.vatRateService = vatRateService;
    }

    @Override
    public void run(String... args) {
        if (!stockItemService.getStockItems(null, null).isEmpty()) {
            logger.info("Ya hay artículos de compra. Se omite la carga de demo de compras.");
            return;
        }
        categories = categoryService.getCategories().stream()
                .collect(Collectors.toMap(ItemCategoryDTO::name, ItemCategoryDTO::id));
        suppliers = supplierService.getSuppliers(null).stream()
                .collect(Collectors.toMap(SupplierDTO::legalName, SupplierDTO::id));
        vatRates = vatRateService.getVatRates().stream()
                .collect(Collectors.toMap(VatRateDTO::name, VatRateDTO::id));
        if (categories.isEmpty() || suppliers.isEmpty()) {
            logger.info("Faltan categorías o proveedores. Se omite la carga de demo de compras.");
            return;
        }

        String drinks = "Bebidas del Norte SL";
        String butcher = "Cárnicas Sierra SA";
        String grocer = "Frutas y Ultramarinos García SL";
        List<ItemUnitConversionDTO> box24 = List.of(new ItemUnitConversionDTO("caja", "ud", new BigDecimal("24")));

        item("AGUA-50", "Agua mineral 50 cl", "Bebidas", "caja", "ud", "ud", box24, "Reducido",
                "48", "144", false, Set.of(), drinks, "caja", "6.00");
        item("COCA-33", "Coca-Cola 33 cl", "Bebidas", "caja", "ud", "ud", box24, "General",
                "48", "120", false, Set.of(), drinks, "caja", "14.40");
        item("CERV-EG33", "Cerveza Estrella Galicia 33 cl", "Bebidas", "caja", "ud", "ud", box24, "General",
                "48", "144", false, Set.of(Allergen.GLUTEN), drinks, "caja", "16.80");
        item("TERN-PIC", "Carne picada de ternera", "Carnes", "kg", "kg", "g", List.of(), "Reducido",
                "3", "10", true, Set.of(), butcher, "kg", "9.50");
        item("POLLO-PEC", "Pechuga de pollo", "Carnes", "kg", "kg", "g", List.of(), "Reducido",
                "3", "8", true, Set.of(), butcher, "kg", "6.20");
        item("BACON", "Bacon en lonchas", "Carnes", "kg", "kg", "g", List.of(), "Reducido",
                "1", "4", true, Set.of(), butcher, "kg", "8.40");
        item("LECHE", "Leche entera", "Lácteos y huevos", "caja", "l", "ml",
                List.of(new ItemUnitConversionDTO("caja", "l", new BigDecimal("6"))), "Superreducido",
                "6", "24", true, Set.of(Allergen.MILK), grocer, "caja", "5.40");
        item("HUEVOS", "Huevos camperos L", "Lácteos y huevos", "doc", "ud", "ud", List.of(), "Superreducido",
                "24", "72", true, Set.of(Allergen.EGGS), grocer, "doc", "2.60");
        item("MOZZ", "Queso mozzarella", "Lácteos y huevos", "kg", "kg", "g", List.of(), "Reducido",
                "2", "6", true, Set.of(Allergen.MILK), grocer, "kg", "7.80");
        item("HARINA", "Harina de trigo", "Secos y conservas", "kg", "kg", "g", List.of(), "Superreducido",
                "5", "25", false, Set.of(Allergen.GLUTEN), grocer, "kg", "0.85");
        item("PATATA", "Patata", "Frutas y verduras", "kg", "kg", "g", List.of(), "Superreducido",
                "10", "40", false, Set.of(), grocer, "kg", "0.90");
        item("ACEITE-OV", "Aceite de oliva virgen extra", "Secos y conservas", "l", "l", "ml", List.of(),
                "Superreducido", "5", "20", false, Set.of(), grocer, "l", "7.50");
        item("TOMATE-TR", "Tomate triturado", "Secos y conservas", "kg", "kg", "g", List.of(), "Reducido",
                "3", "12", false, Set.of(), grocer, "kg", "1.40");

        logger.info("Artículos de compra de demo cargados: {}", stockItemService.getStockItems(null, null).size());
    }

    private void item(String code, String name, String category, String purchaseUnit, String stockUnit,
                      String usageUnit, List<ItemUnitConversionDTO> conversions, String vatRate, String min,
                      String max, boolean perishable, Set<Allergen> allergens, String supplier, String tariffUnit,
                      String tariffPrice) {
        Long supplierId = suppliers.get(supplier);
        StockItemDTO created = stockItemService.createStockItem(new StockItemDTO(null, code, name,
                categories.get(category), null, purchaseUnit, stockUnit, usageUnit, conversions, supplierId,
                vatRates.get(vatRate), new BigDecimal(min), new BigDecimal(max), perishable, allergens, true));
        stockItemService.setSupplierPrice(created.id(), supplierId,
                new StockItemSupplierDTO(null, null, null, tariffUnit, new BigDecimal(tariffPrice), null));
    }
}
