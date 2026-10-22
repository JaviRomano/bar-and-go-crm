package com.barandgo.crm.inventory.internal;

import org.springframework.stereotype.Component;

import com.barandgo.crm.purchasing.StockItemUsageCheck;

@Component
class LedgerStockItemUsageCheck implements StockItemUsageCheck {

    private final StockMovementRepository movementRepository;

    LedgerStockItemUsageCheck(StockMovementRepository movementRepository) {
        this.movementRepository = movementRepository;
    }

    @Override
    public boolean hasStockMovements(Long stockItemId) {
        return movementRepository.existsByStockItemId(stockItemId);
    }
}
