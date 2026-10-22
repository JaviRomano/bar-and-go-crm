package com.barandgo.crm.inventory.internal;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockBalanceRepository extends JpaRepository<StockBalance, StockBalance.Key> {

    // Crea la fila a cero si no existe; con ON CONFLICT no choca con otra transacción que haga lo mismo
    @Modifying
    @Query(value = "INSERT INTO stock_balance (stock_item_id, warehouse_id, quantity) "
            + "VALUES (:itemId, :warehouseId, 0) ON CONFLICT DO NOTHING", nativeQuery = true)
    void ensureExists(@Param("itemId") Long itemId, @Param("warehouseId") Long warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from StockBalance b where b.id = :key")
    Optional<StockBalance> lockByKey(@Param("key") StockBalance.Key key);

    List<StockBalance> findByIdWarehouseId(Long warehouseId);

    List<StockBalance> findByIdStockItemId(Long stockItemId);
}
