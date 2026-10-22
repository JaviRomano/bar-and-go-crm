package com.barandgo.crm.catalog.internal;

import com.barandgo.crm.catalog.ProductDTO;
import com.barandgo.crm.catalog.Product;
import com.barandgo.crm.shared.BadRequestException;
import com.barandgo.crm.shared.ResourceNotFoundException;
import com.barandgo.crm.ordering.internal.OrderItemRepository;
import com.barandgo.crm.catalog.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;

    public ProductServiceImpl(ProductRepository productRepository, ProductCategoryRepository categoryRepository,
                              OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public ProductDTO createProduct(ProductDTO productDTO) {
        Product product = new Product();
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setCategory(findActiveCategory(productDTO.getCategoryId()));
        product.setAvailable(productDTO.getAvailable() != null ? productDTO.getAvailable() : true);
        product.setImageUrl(productDTO.getImageUrl());

        Product savedProduct = productRepository.save(product);
        logger.info("Producto {} creado", savedProduct.getId());

        return convertToDTO(savedProduct);
    }

    // Incluye archivados: un pedido antiguo puede necesitar consultar su producto
    @Override
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        return convertToDTO(findProduct(id));
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
        if (productDTO.getAvailable() != null) {
            product.setAvailable(productDTO.getAvailable());
        }
        if (productDTO.getImageUrl() != null) {
            product.setImageUrl(productDTO.getImageUrl());
        }

        Product updatedProduct = productRepository.save(product);
        logger.info("Producto {} actualizado", updatedProduct.getId());

        return convertToDTO(updatedProduct);
    }

    /**
     * Un producto que ya se ha vendido no se borra: se archiva para que los pedidos
     * sigan apuntando a él. Si nunca se ha vendido, se borra.
     */
    @Override
    public void deleteProduct(Long id) {
        Product product = findProduct(id);

        if (orderItemRepository.existsByProductId(id)) {
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

        return convertToDTO(updatedProduct);
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

    private List<ProductDTO> toDTOList(List<Product> products) {
        return products.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setCategoryId(product.getCategory().getId());
        dto.setCategory(product.getCategory().getName());
        dto.setAvailable(product.getAvailable());
        dto.setImageUrl(product.getImageUrl());
        return dto;
    }
}
