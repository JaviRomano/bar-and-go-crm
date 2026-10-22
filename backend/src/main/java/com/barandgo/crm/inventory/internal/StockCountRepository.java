package com.barandgo.crm.inventory.internal;

import com.barandgo.crm.inventory.StockCountDTOs.StockCountStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockCountRepository extends JpaRepository<StockCount, Long> {

    List<StockCount> findAllByOrderByIdDesc();

    boolean existsByWarehouseIdAndStatusNot(Long warehouseId, StockCountStatus status);
}
