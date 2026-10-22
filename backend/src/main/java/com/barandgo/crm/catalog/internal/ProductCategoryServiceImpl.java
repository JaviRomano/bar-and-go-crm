package com.barandgo.crm.catalog.internal;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barandgo.crm.catalog.ProductCategoryDTO;
import com.barandgo.crm.shared.ConflictException;
import com.barandgo.crm.shared.ResourceNotFoundException;
import com.barandgo.crm.catalog.ProductCategoryService;

@Service
@Transactional
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private static final Logger logger = LoggerFactory.getLogger(ProductCategoryServiceImpl.class);

    private final ProductCategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public ProductCategoryServiceImpl(ProductCategoryRepository categoryRepository,
                                      ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCategoryDTO> getCategories(boolean includeInactive) {
        List<ProductCategory> categories = includeInactive
                ? categoryRepository.findAllByOrderBySortOrderAscNameAsc()
                : categoryRepository.findByActiveTrueOrderBySortOrderAscNameAsc();
        return categories.stream().map(ProductCategoryDTO::from).toList();
    }

    @Override
    public ProductCategoryDTO createCategory(ProductCategoryDTO categoryDTO) {
        String name = categoryDTO.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Ya existe una categoría con el nombre '" + name + "'");
        }

        ProductCategory category = new ProductCategory();
        category.setName(name);
        category.setSortOrder(categoryDTO.sortOrder() != null ? categoryDTO.sortOrder() : 0);
        category.setActive(categoryDTO.active() != null ? categoryDTO.active() : true);

        ProductCategory saved = categoryRepository.save(category);
        logger.info("Categoría de carta {} creada", saved.getId());
        return ProductCategoryDTO.from(saved);
    }

    @Override
    public ProductCategoryDTO updateCategory(Long id, ProductCategoryDTO categoryDTO) {
        ProductCategory category = findCategory(id);
        String name = categoryDTO.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Ya existe una categoría con el nombre '" + name + "'");
        }

        category.setName(name);
        if (categoryDTO.sortOrder() != null) {
            category.setSortOrder(categoryDTO.sortOrder());
        }
        if (categoryDTO.active() != null) {
            category.setActive(categoryDTO.active());
        }

        logger.info("Categoría de carta {} actualizada", id);
        return ProductCategoryDTO.from(categoryRepository.save(category));
    }

    @Override
    public void deleteCategory(Long id) {
        findCategory(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException("La categoría tiene productos asociados. Desactívala en lugar de borrarla");
        }
        categoryRepository.deleteById(id);
        logger.info("Categoría de carta {} eliminada", id);
    }

    private ProductCategory findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));
    }
}
