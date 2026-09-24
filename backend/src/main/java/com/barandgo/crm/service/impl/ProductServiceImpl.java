package com.barandgo.crm.service.impl;

import com.barandgo.crm.dto.product.ProductDTO;
import com.barandgo.crm.entity.Product;
import com.barandgo.crm.exception.ResourceNotFoundException;
import com.barandgo.crm.repository.ProductRepository;
import com.barandgo.crm.service.ProductService;
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
    
    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    
    @Override
    public ProductDTO createProduct(ProductDTO productDTO) {
        Product product = new Product();
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setCategory(productDTO.getCategory());
        product.setAvailable(productDTO.getAvailable() != null ? productDTO.getAvailable() : true);
        product.setImageUrl(productDTO.getImageUrl());
        
        Product savedProduct = productRepository.save(product);
        logger.info("Producto {} creado", savedProduct.getId());
        
        return convertToDTO(savedProduct);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
        
        return convertToDTO(product);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getAvailableProducts() {
        return productRepository.findByAvailable(true).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategory(String category) {
        return productRepository.findByCategoryAndAvailable(category, true).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
        
        if (productDTO.getName() != null) {
            product.setName(productDTO.getName());
        }
        if (productDTO.getDescription() != null) {
            product.setDescription(productDTO.getDescription());
        }
        if (productDTO.getPrice() != null) {
            product.setPrice(productDTO.getPrice());
        }
        if (productDTO.getCategory() != null) {
            product.setCategory(productDTO.getCategory());
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
    
    @Override
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Producto no encontrado con ID: " + id);
        }
        
        productRepository.deleteById(id);
        logger.info("Producto {} eliminado", id);
    }
    
    @Override
    public ProductDTO toggleAvailability(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
        
        product.setAvailable(!product.getAvailable());
        Product updatedProduct = productRepository.save(product);
        
        logger.info("Disponibilidad del producto {} cambiada a: {}", id, updatedProduct.getAvailable());
        
        return convertToDTO(updatedProduct);
    }
    
    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setCategory(product.getCategory());
        dto.setAvailable(product.getAvailable());
        dto.setImageUrl(product.getImageUrl());
        return dto;
    }
}