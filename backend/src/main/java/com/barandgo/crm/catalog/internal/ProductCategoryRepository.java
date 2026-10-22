package com.barandgo.crm.catalog.internal;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    List<ProductCategory> findAllByOrderBySortOrderAscNameAsc();

    List<ProductCategory> findByActiveTrueOrderBySortOrderAscNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
