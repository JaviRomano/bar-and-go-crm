package com.barandgo.crm.masterdata;

import java.util.List;


public interface ItemCategoryService {

    List<ItemCategoryDTO> getCategories();

    ItemCategoryDTO createCategory(ItemCategoryDTO categoryDTO);

    ItemCategoryDTO updateCategory(Long id, ItemCategoryDTO categoryDTO);

    void deleteCategory(Long id);
}
