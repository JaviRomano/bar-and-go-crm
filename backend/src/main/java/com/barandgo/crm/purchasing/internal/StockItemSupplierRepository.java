package com.barandgo.crm.purchasing.internal;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockItemSupplierRepository extends JpaRepository<StockItemSupplier, Long> {

    List<StockItemSupplier> findByStockItemIdOrderByUnitPriceAsc(Long stockItemId);

    Optional<StockItemSupplier> findByStockItemIdAndSupplierId(Long stockItemId, Long supplierId);
}
