package com.barandgo.crm.masterdata.internal;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barandgo.crm.masterdata.ItemCategoryDTO;
import com.barandgo.crm.shared.ConflictException;
import com.barandgo.crm.shared.ResourceNotFoundException;
import com.barandgo.crm.masterdata.ItemCategoryService;

@Service
@Transactional
public class ItemCategoryServiceImpl implements ItemCategoryService {

    private static final Logger logger = LoggerFactory.getLogger(ItemCategoryServiceImpl.class);

    private final ItemCategoryRepository categoryRepository;

    public ItemCategoryServiceImpl(ItemCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemCategoryDTO> getCategories() {
        return categoryRepository.findAllByOrderByNameAsc().stream().map(ItemCategoryDTO::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ItemCategoryDTO getCategory(Long id) {
        return ItemCategoryDTO.from(findCategory(id));
    }

    @Override
    public ItemCategoryDTO createCategory(ItemCategoryDTO categoryDTO) {
        String name = categoryDTO.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Ya existe una categoría con el nombre '" + name + "'");
        }

        ItemCategory category = new ItemCategory();
        category.setName(name);
        category.setActive(categoryDTO.active() != null ? categoryDTO.active() : true);

        ItemCategory saved = categoryRepository.save(category);
        logger.info("Categoría de artículo {} creada", saved.getId());
        return ItemCategoryDTO.from(saved);
    }

    @Override
    public ItemCategoryDTO updateCategory(Long id, ItemCategoryDTO categoryDTO) {
        ItemCategory category = findCategory(id);
        String name = categoryDTO.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Ya existe una categoría con el nombre '" + name + "'");
        }

        category.setName(name);
        if (categoryDTO.active() != null) {
            category.setActive(categoryDTO.active());
        }

        logger.info("Categoría de artículo {} actualizada", id);
        return ItemCategoryDTO.from(categoryRepository.save(category));
    }

    /*
     * Una categoría con artículos no se puede borrar: lo impide la FK de stock_item. Este módulo no
     * conoce los artículos (dependería de compras), así que se deja actuar a la FK y se fuerza el
     * flush aquí para que la violación salga como 409 y no al hacer commit.
     */
    @Override
    public void deleteCategory(Long id) {
        findCategory(id);
        categoryRepository.deleteById(id);
        try {
            categoryRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("La categoría tiene artículos asociados. Desactívala en lugar de borrarla");
        }
        logger.info("Categoría de artículo {} eliminada", id);
    }

    private ItemCategory findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));
    }
}
