package com.barandgo.crm.service;

import java.util.List;

import com.barandgo.crm.dto.category.ProductCategoryDTO;

public interface ProductCategoryService {

    List<ProductCategoryDTO> getCategories(boolean includeInactive);

    ProductCategoryDTO createCategory(ProductCategoryDTO categoryDTO);

    ProductCategoryDTO updateCategory(Long id, ProductCategoryDTO categoryDTO);

    void deleteCategory(Long id);
}
