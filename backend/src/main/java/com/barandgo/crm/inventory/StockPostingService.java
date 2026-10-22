package com.barandgo.crm.inventory;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.inventory.InventoryDTOs.StockMovementDTO;

/**
 * Movimientos de stock que originan otros módulos: producción (escandallos) y, en la Fase 5, ventas y
 * mermas. Recepciones, ajustes y traspasos tienen su propio flujo y no pasan por aquí.
 *
 * <p>Se ejecuta en la transacción del llamante: si su operación falla, los movimientos tampoco quedan.
 */
public interface StockPostingService {

    /**
     * @param quantity con signo, en unidad de inventario del artículo
     * @param unitCost solo para entradas; null valora al PMP vigente
     */
    record PostingLine(Long stockItemId, BigDecimal quantity, BigDecimal unitCost) {
    }

    /** totalCost: suma con signo del valor de los movimientos (negativa en consumos). */
    record PostingResult(UUID operationId, List<StockMovementDTO> movements, BigDecimal totalCost) {
    }

    /**
     * @param operationId agrupa estos movimientos con otros de la misma operación (la producción consume
     *                    y da de alta con el mismo); null genera uno nuevo
     */
    PostingResult post(UUID operationId, MovementType type, MovementSource source, Long sourceId, Long warehouseId,
                       List<PostingLine> lines, String notes, CurrentUser user);
}
