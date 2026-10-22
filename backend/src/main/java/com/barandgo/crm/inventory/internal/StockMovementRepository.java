package com.barandgo.crm.inventory.internal;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.barandgo.crm.inventory.MovementSource;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long>,
        JpaSpecificationExecutor<StockMovement> {

    boolean existsByStockItemId(Long stockItemId);

    List<StockMovement> findBySourceTypeAndSourceIdOrderById(MovementSource sourceType, Long sourceId);
}
