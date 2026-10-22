package com.barandgo.crm.inventory.internal;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockValuationRepository extends JpaRepository<StockValuation, StockValuation.Key> {

    @Modifying
    @Query(value = "INSERT INTO stock_valuation (stock_item_id, site_id, quantity_on_hand, avg_cost) "
            + "VALUES (:itemId, :siteId, 0, 0) ON CONFLICT DO NOTHING", nativeQuery = true)
    void ensureExists(@Param("itemId") Long itemId, @Param("siteId") Long siteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from StockValuation v where v.id = :key")
    Optional<StockValuation> lockByKey(@Param("key") StockValuation.Key key);
}
