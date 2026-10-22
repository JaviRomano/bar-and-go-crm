package com.barandgo.crm.inventory.internal;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockLotRepository extends JpaRepository<StockLot, Long> {

    @Query("select l from StockLot l where l.stockItemId = :itemId and l.warehouseId = :warehouseId "
            + "and l.remainingQuantity > 0")
    List<StockLot> findOpenLots(@Param("itemId") Long itemId, @Param("warehouseId") Long warehouseId);

    @Query("select l from StockLot l where l.remainingQuantity > 0 and l.expiryDate <= :limit "
            + "order by l.expiryDate, l.id")
    List<StockLot> findExpiringUntil(@Param("limit") LocalDate limit);
}
