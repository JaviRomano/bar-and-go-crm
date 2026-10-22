package com.barandgo.crm.inventory.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long>,
        JpaSpecificationExecutor<StockMovement> {

    boolean existsByStockItemId(Long stockItemId);
}
