package com.barandgo.crm.catalog.internal;

import com.barandgo.crm.catalog.ProductAllergenSource;
import com.barandgo.crm.catalog.ProductDTO;
import com.barandgo.crm.catalog.Product;
import com.barandgo.crm.masterdata.Allergen;
import com.barandgo.crm.masterdata.VatRateDTO;
import com.barandgo.crm.masterdata.VatRateService;
import com.barandgo.crm.shared.BadRequestException;
import com.barandgo.crm.shared.ResourceNotFoundException;
import com.barandgo.crm.catalog.ProductSalesCheck;
import com.barandgo.crm.catalog.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    // Tipo por defecto si no se indica: el del 10 %, el habitual en hostelería
    private static final BigDecimal DEFAULT_VAT_RATE = new BigDecimal("10.00");

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductSalesCheck productSalesCheck;
    private final ProductAllergenSource allergenSource;
    private final VatRateService vatRateService;

    public ProductServiceImpl(ProductRepository productRepository, ProductCategoryRepository categoryRepository,
                              ProductSalesCheck productSalesCheck, ProductAllergenSource allergenSource,
                              VatRateService vatRateService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productSalesCheck = productSalesCheck;
        this.allergenSource = allergenSource;
        this.vatRateService = vatRateService;
    }

    @Override
    public ProductDTO createProduct(ProductDTO productDTO) {
        Product product = new Product();
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setCategory(findActiveCategory(productDTO.getCategoryId()));
        product.setVatRateId(productDTO.getVatRateId() != null
                ? activeVatRate(productDTO.getVatRateId()).id()
                : defaultVatRate().id());
        product.setAvailable(productDTO.getAvailable() != null ? productDTO.getAvailable() : true);
        product.setImageUrl(productDTO.getImageUrl());

        Product savedProduct = productRepository.save(product);
        logger.info("Producto {} creado", savedProduct.getId());

        return toDTOList(List.of(savedProduct)).get(0);
    }

    // Incluye archivados: un pedido antiguo puede necesitar consultar su producto
    @Override
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        return toDTOList(List.of(findProduct(id))).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public Product getSellableProduct(Long id) {
        Product product = findProduct(id);
        if (!Boolean.TRUE.equals(product.getAvailable()) || !Boolean.TRUE.equals(product.getActive())) {
            throw new BadRequestException("El producto '" + product.getName() + "' no está disponible");
        }
        return product;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        return toDTOList(productRepository.findByActiveTrueOrderByCategorySortOrderAscNameAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getAvailableProducts() {
        return toDTOList(productRepository.findByActiveTrueAndAvailableTrueOrderByCategorySortOrderAscNameAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(String category) {
        return toDTOList(productRepository.findByCategoryNameAndActiveTrueAndAvailableTrueOrderByNameAsc(category));
    }

    @Override
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product product = findProduct(id);

        if (productDTO.getName() != null) {
            product.setName(productDTO.getName());
        }
        if (productDTO.getDescription() != null) {
            product.setDescription(productDTO.getDescription());
        }
        if (productDTO.getPrice() != null) {
            product.setPrice(productDTO.getPrice());
        }
        if (productDTO.getCategoryId() != null
                && !productDTO.getCategoryId().equals(product.getCategory().getId())) {
            product.setCategory(findActiveCategory(productDTO.getCategoryId()));
        }
        if (productDTO.getVatRateId() != null && !productDTO.getVatRateId().equals(product.getVatRateId())) {
            product.setVatRateId(activeVatRate(productDTO.getVatRateId()).id());
        }
        if (productDTO.getAvailable() != null) {
            product.setAvailable(productDTO.getAvailable());
        }
        if (productDTO.getImageUrl() != null) {
            product.setImageUrl(productDTO.getImageUrl());
        }

        Product updatedProduct = productRepository.save(product);
        logger.info("Producto {} actualizado", updatedProduct.getId());

        return toDTOList(List.of(updatedProduct)).get(0);
    }

    /**
     * Un producto que ya se ha vendido no se borra: se archiva para que los pedidos
     * sigan apuntando a él. Si nunca se ha vendido, se borra.
     */
    @Override
    public void deleteProduct(Long id) {
        Product product = findProduct(id);

        if (productSalesCheck.hasSales(id)) {
            product.setActive(false);
            product.setAvailable(false);
            productRepository.save(product);
            logger.info("Producto {} archivado: tiene ventas registradas", id);
            return;
        }

        productRepository.delete(product);
        logger.info("Producto {} eliminado", id);
    }

    @Override
    public ProductDTO toggleAvailability(Long id) {
        Product product = findProduct(id);

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new BadRequestException("El producto está archivado y no puede volver a la carta");
        }

        product.setAvailable(!product.getAvailable());
        Product updatedProduct = productRepository.save(product);

        logger.info("Disponibilidad del producto {} cambiada a: {}", id, updatedProduct.getAvailable());

        return toDTOList(List.of(updatedProduct)).get(0);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
    }

    private ProductCategory findActiveCategory(Long categoryId) {
        ProductCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + categoryId));
        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new BadRequestException("La categoría '" + category.getName() + "' está desactivada");
        }
        return category;
    }

    private VatRateDTO activeVatRate(Long vatRateId) {
        VatRateDTO vatRate = vatRateService.getVatRate(vatRateId);
        if (!Boolean.TRUE.equals(vatRate.active())) {
            throw new BadRequestException("El tipo de IVA '" + vatRate.name() + "' está desactivado");
        }
        return vatRate;
    }

    private VatRateDTO defaultVatRate() {
        return vatRateService.getVatRates().stream()
                .filter(rate -> Boolean.TRUE.equals(rate.active()) && rate.rate().compareTo(DEFAULT_VAT_RATE) == 0)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No hay un tipo de IVA del 10 % activo: indica vatRateId"));
    }

    // Alérgenos e IVA de todos los productos con una consulta cada uno, no una por producto
    private List<ProductDTO> toDTOList(List<Product> products) {
        Map<Long, Set<Allergen>> allergens = allergenSource.allergensByProduct(
                products.stream().map(Product::getId).toList());
        Map<Long, BigDecimal> vatRates = vatRateService.getVatRates().stream()
                .collect(Collectors.toMap(VatRateDTO::id, VatRateDTO::rate));
        return products.stream()
                .map(product -> convertToDTO(product,
                        allergens.getOrDefault(product.getId(), EnumSet.noneOf(Allergen.class)),
                        vatRates.get(product.getVatRateId())))
                .collect(Collectors.toList());
    }

    private ProductDTO convertToDTO(Product product, Set<Allergen> allergens, BigDecimal vatRate) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setCategoryId(product.getCategory().getId());
        dto.setCategory(product.getCategory().getName());
        dto.setVatRateId(product.getVatRateId());
        dto.setVatRate(vatRate);
        dto.setAllergens(allergens);
        dto.setAvailable(product.getAvailable());
        dto.setImageUrl(product.getImageUrl());
        return dto;
    }
}
