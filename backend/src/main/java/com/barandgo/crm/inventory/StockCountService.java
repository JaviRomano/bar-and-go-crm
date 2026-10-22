package com.barandgo.crm.inventory;

import java.util.List;

import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.inventory.StockCountDTOs.CountEntriesRequest;
import com.barandgo.crm.inventory.StockCountDTOs.OpenCountRequest;
import com.barandgo.crm.inventory.StockCountDTOs.StockCountDTO;

/**
 * Recuentos: OPEN -> IN_PROGRESS (al registrar cantidades) -> CLOSED. Al cerrar, cada diferencia entre
 * lo contado y el stock teórico genera un ajuste. Las líneas sin contar no se ajustan.
 */
public interface StockCountService {

    StockCountDTO openCount(OpenCountRequest request, CurrentUser requester);

    StockCountDTO recordCounts(Long countId, CountEntriesRequest request, CurrentUser requester);

    StockCountDTO closeCount(Long countId, CurrentUser requester);

    StockCountDTO getCount(Long countId, CurrentUser requester);

    /** Filtro opcional por almacén; más recientes primero. */
    List<StockCountDTO> getCounts(Long warehouseId, CurrentUser requester);
}
