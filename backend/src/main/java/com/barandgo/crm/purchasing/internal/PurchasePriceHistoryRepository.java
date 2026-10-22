package com.barandgo.crm.purchasing.internal;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchasePriceHistoryRepository extends JpaRepository<PurchasePriceHistory, Long> {

    List<PurchasePriceHistory> findByStockItemIdOrderByPurchasedOnDescIdDesc(Long stockItemId);
}
