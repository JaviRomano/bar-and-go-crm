package com.barandgo.crm.recipes.internal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    List<Recipe> findAllByOrderByNameAsc();

    List<Recipe> findByProductIdIn(Collection<Long> productIds);

    Optional<Recipe> findByProductId(Long productId);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByProductIdAndIdNot(Long productId, Long id);

    boolean existsByStockItemIdAndIdNot(Long stockItemId, Long id);
}
