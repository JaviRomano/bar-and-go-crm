package com.barandgo.crm.catalog;

import java.util.List;


public interface ProductCategoryService {

    List<ProductCategoryDTO> getCategories(boolean includeInactive);

    ProductCategoryDTO createCategory(ProductCategoryDTO categoryDTO);

    ProductCategoryDTO updateCategory(Long id, ProductCategoryDTO categoryDTO);

    void deleteCategory(Long id);
}
