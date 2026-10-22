package com.barandgo.crm.catalog.internal;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.barandgo.crm.catalog.Product;

/**
 * Carga la carta de demostración si no hay productos. Solo en el perfil dev.
 * Las categorías las crea la migración V4; aquí solo se buscan por nombre.
 */
@Component
@Profile("dev")
class DemoMenuInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DemoMenuInitializer.class);

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;

    private Map<String, ProductCategory> categoriesByName;

    DemoMenuInitializer(ProductRepository productRepository, ProductCategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            logger.info("La carta ya tiene productos. Se omite la carga de demo.");
            return;
        }

        initializeProducts();
        logger.info("Carta de demo cargada: {} productos", productRepository.count());
    }

    private void initializeProducts() {
        categoriesByName = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(ProductCategory::getName, Function.identity()));

        // ENTRANTES
        createProduct("Croquetas de Jamón", "Croquetas caseras de jamón ibérico (8 unidades)", 
                     new BigDecimal("8.50"), "ENTRANTES");
        createProduct("Patatas Bravas", "Patatas fritas con salsa brava y alioli", 
                     new BigDecimal("6.00"), "ENTRANTES");
        createProduct("Ensalada César", "Lechuga romana, pollo, parmesano, crutones y salsa césar", 
                     new BigDecimal("9.50"), "ENTRANTES");
        createProduct("Nachos con Guacamole", "Nachos crujientes con guacamole casero, queso y jalapeños", 
                     new BigDecimal("7.50"), "ENTRANTES");
        createProduct("Alitas de Pollo BBQ", "Alitas de pollo con salsa barbacoa (8 unidades)", 
                     new BigDecimal("9.00"), "ENTRANTES");
        createProduct("Tabla de Quesos", "Selección de quesos españoles con mermelada y frutos secos", 
                     new BigDecimal("12.00"), "ENTRANTES");
        
        // PRINCIPALES
        createProduct("Hamburguesa Clásica", "Hamburguesa de ternera 200g con lechuga, tomate, cebolla y patatas", 
                     new BigDecimal("11.50"), "PRINCIPALES");
        createProduct("Hamburguesa BBQ", "Hamburguesa con bacon, cheddar, cebolla caramelizada y salsa BBQ", 
                     new BigDecimal("13.00"), "PRINCIPALES");
        createProduct("Pizza Margarita", "Tomate, mozzarella y albahaca fresca", 
                     new BigDecimal("9.00"), "PRINCIPALES");
        createProduct("Pizza Cuatro Quesos", "Mozzarella, gorgonzola, parmesano y queso de cabra", 
                     new BigDecimal("11.50"), "PRINCIPALES");
        createProduct("Pizza Carbonara", "Nata, bacon, champiñones, huevo y parmesano", 
                     new BigDecimal("12.00"), "PRINCIPALES");
        createProduct("Sándwich Club", "Pollo, bacon, lechuga, tomate, huevo y mayonesa con patatas", 
                     new BigDecimal("10.00"), "PRINCIPALES");
        createProduct("Pasta Carbonara", "Espaguetis con bacon, huevo, nata y parmesano", 
                     new BigDecimal("10.50"), "PRINCIPALES");
        createProduct("Pasta Boloñesa", "Espaguetis con salsa de carne bolognesa casera", 
                     new BigDecimal("10.00"), "PRINCIPALES");
        createProduct("Lasaña de Carne", "Lasaña casera con carne, bechamel y queso gratinado", 
                     new BigDecimal("11.00"), "PRINCIPALES");
        createProduct("Pollo a la Plancha", "Pechuga de pollo a la plancha con ensalada y patatas", 
                     new BigDecimal("11.50"), "PRINCIPALES");
        
        // POSTRES
        createProduct("Tarta de Queso", "Tarta de queso casera con base de galleta", 
                     new BigDecimal("5.50"), "POSTRES");
        createProduct("Brownie con Helado", "Brownie de chocolate caliente con helado de vainilla", 
                     new BigDecimal("6.00"), "POSTRES");
        createProduct("Tiramisú", "Tiramisú italiano casero", 
                     new BigDecimal("5.50"), "POSTRES");
        createProduct("Coulant de Chocolate", "Coulant de chocolate con corazón fundido y helado", 
                     new BigDecimal("6.50"), "POSTRES");
        createProduct("Helado Artesanal", "2 bolas de helado artesanal (sabores variados)", 
                     new BigDecimal("4.50"), "POSTRES");
        
        // BEBIDAS
        createProduct("Coca-Cola", "Coca-Cola (33cl)", new BigDecimal("2.50"), "BEBIDAS");
        createProduct("Coca-Cola Zero", "Coca-Cola Zero (33cl)", new BigDecimal("2.50"), "BEBIDAS");
        createProduct("Fanta Naranja", "Fanta Naranja (33cl)", new BigDecimal("2.50"), "BEBIDAS");
        createProduct("Nestea", "Nestea Limón (33cl)", new BigDecimal("2.50"), "BEBIDAS");
        createProduct("Agua Mineral", "Agua mineral (50cl)", new BigDecimal("2.00"), "BEBIDAS");
        createProduct("Cerveza Estrella Galicia", "Cerveza Estrella Galicia (33cl)", 
                     new BigDecimal("2.80"), "BEBIDAS");
        createProduct("Cerveza Mahou", "Cerveza Mahou 5 Estrellas (33cl)", 
                     new BigDecimal("2.50"), "BEBIDAS");
        createProduct("Vino Tinto Copa", "Copa de vino tinto de la casa", 
                     new BigDecimal("3.50"), "BEBIDAS");
        createProduct("Vino Blanco Copa", "Copa de vino blanco de la casa", 
                     new BigDecimal("3.50"), "BEBIDAS");
        createProduct("Café Solo", "Café solo", new BigDecimal("1.50"), "BEBIDAS");
        createProduct("Café con Leche", "Café con leche", new BigDecimal("1.80"), "BEBIDAS");
        createProduct("Zumo Natural Naranja", "Zumo de naranja natural", 
                     new BigDecimal("3.50"), "BEBIDAS");
    }
    
    private void createProduct(String name, String description, BigDecimal price, String category) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        ProductCategory productCategory = categoriesByName.get(category);
        if (productCategory == null) {
            throw new IllegalStateException("Falta la categoría de carta '" + category + "' (la crea la migración V4)");
        }
        product.setCategory(productCategory);
        product.setAvailable(true);
        productRepository.save(product);
    }
}
