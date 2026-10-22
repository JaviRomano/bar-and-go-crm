package com.barandgo.crm.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barandgo.crm.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Carta completa (sin archivados), en el orden de las categorías
    List<Product> findByActiveTrueOrderByCategorySortOrderAscNameAsc();

    List<Product> findByActiveTrueAndAvailableTrueOrderByCategorySortOrderAscNameAsc();

    List<Product> findByCategoryNameAndActiveTrueAndAvailableTrueOrderByNameAsc(String categoryName);

    boolean existsByCategoryId(Long categoryId);
}
