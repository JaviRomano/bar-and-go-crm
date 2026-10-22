package com.barandgo.crm.masterdata;

import java.util.List;


public interface ItemCategoryService {

    List<ItemCategoryDTO> getCategories();

    /**
     * @throws com.barandgo.crm.shared.ResourceNotFoundException si no existe
     */
    ItemCategoryDTO getCategory(Long id);

    ItemCategoryDTO createCategory(ItemCategoryDTO categoryDTO);

    ItemCategoryDTO updateCategory(Long id, ItemCategoryDTO categoryDTO);

    void deleteCategory(Long id);
}
