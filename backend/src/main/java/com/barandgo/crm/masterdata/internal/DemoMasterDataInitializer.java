package com.barandgo.crm.masterdata.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.barandgo.crm.masterdata.ItemCategoryDTO;
import com.barandgo.crm.masterdata.ItemCategoryService;
import com.barandgo.crm.masterdata.SupplierDTO;
import com.barandgo.crm.masterdata.SupplierService;

/**
 * Categorías de compra y proveedores de demostración. Solo en el perfil dev y si no hay proveedores.
 * Los CIF son válidos en formato y dígito de control, pero ficticios.
 */
@Component
@Profile("dev")
@Order(10)
class DemoMasterDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DemoMasterDataInitializer.class);

    private final ItemCategoryService categoryService;
    private final SupplierService supplierService;

    DemoMasterDataInitializer(ItemCategoryService categoryService, SupplierService supplierService) {
        this.categoryService = categoryService;
        this.supplierService = supplierService;
    }

    @Override
    public void run(String... args) {
        if (!supplierService.getSuppliers(null).isEmpty()) {
            logger.info("Ya hay proveedores. Se omite la carga de demo de datos maestros.");
            return;
        }

        for (String name : new String[] {"Bebidas", "Carnes", "Lácteos y huevos", "Frutas y verduras",
                "Secos y conservas", "Limpieza"}) {
            categoryService.createCategory(new ItemCategoryDTO(null, name, true));
        }

        supplier("B12345674", "Bebidas del Norte SL", "Marta Ruiz", "+34944000111", "30 días fecha factura", 2);
        supplier("A58818501", "Cárnicas Sierra SA", "Luis Gómez", "+34918000222", "Contado", 1);
        supplier("B87654323", "Frutas y Ultramarinos García SL", "Ana García", "+34917000333",
                "15 días fecha factura", 1);

        logger.info("Datos maestros de demo cargados: 6 categorías de compra y 3 proveedores");
    }

    private void supplier(String taxId, String legalName, String contact, String phone, String paymentTerms,
                          int leadTimeDays) {
        supplierService.createSupplier(new SupplierDTO(null, taxId, legalName, null, contact, null, phone,
                paymentTerms, leadTimeDays, null, true, null));
    }
}
