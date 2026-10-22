package com.barandgo.crm.catalog;

import java.util.List;

public interface ProductService {
    
    ProductDTO createProduct(ProductDTO productDTO);
    
    ProductDTO getProductById(Long id);
    
    List<ProductDTO> getAllProducts();
    
    List<ProductDTO> getAvailableProducts();
    
    List<ProductDTO> getProductsByCategory(String category);
    
    ProductDTO updateProduct(Long id, ProductDTO productDTO);
    
    void deleteProduct(Long id);
    
    ProductDTO toggleAvailability(Long id);
}