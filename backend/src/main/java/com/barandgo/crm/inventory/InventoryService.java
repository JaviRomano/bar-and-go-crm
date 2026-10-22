package com.barandgo.crm.inventory;

import java.time.Instant;
import java.util.List;

import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.inventory.InventoryDTOs.AdjustmentRequestDTO;
import com.barandgo.crm.inventory.InventoryDTOs.ExpiringLotDTO;
import com.barandgo.crm.inventory.InventoryDTOs.StockBalanceDTO;
import com.barandgo.crm.inventory.InventoryDTOs.StockMovementDTO;
import com.barandgo.crm.inventory.InventoryDTOs.TransferRequestDTO;
import com.barandgo.crm.inventory.InventoryDTOs.ValuationReportDTO;

/**
 * Consultas de stock y operaciones manuales. Todo cambio de stock pasa por un movimiento.
 */
public interface InventoryService {

    /** Filtros opcionales: null no filtra. */
    List<StockBalanceDTO> getStock(Long warehouseId, Long stockItemId);

    ValuationReportDTO getValuation();

    /** Filtros opcionales: null no filtra. Más recientes primero. */
    List<StockMovementDTO> getMovements(Long stockItemId, Long warehouseId, Instant from, Instant to);

    List<StockMovementDTO> adjust(AdjustmentRequestDTO request, CurrentUser requester);

    /** Traspaso entre almacenes del mismo local; los lotes viajan con la mercancía. */
    List<StockMovementDTO> transfer(TransferRequestDTO request, CurrentUser requester);

    /** Lotes con existencias que caducan en los próximos días (incluidos los ya caducados). */
    List<ExpiringLotDTO> getExpiringLots(int days);
}
