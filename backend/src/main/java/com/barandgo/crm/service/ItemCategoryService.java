package com.barandgo.crm.service;

import java.util.List;

import com.barandgo.crm.dto.category.ItemCategoryDTO;

public interface ItemCategoryService {

    List<ItemCategoryDTO> getCategories();

    ItemCategoryDTO createCategory(ItemCategoryDTO categoryDTO);

    ItemCategoryDTO updateCategory(Long id, ItemCategoryDTO categoryDTO);

    void deleteCategory(Long id);
}
