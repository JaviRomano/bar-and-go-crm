package com.barandgo.crm.costcontrol;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.barandgo.crm.costcontrol.CostControlDTOs.ConsumptionReportDTO;
import com.barandgo.crm.costcontrol.CostControlDTOs.OrderStockPostingDTO;
import com.barandgo.crm.costcontrol.CostControlDTOs.WasteDTO;
import com.barandgo.crm.costcontrol.CostControlDTOs.WasteRequest;
import com.barandgo.crm.identity.CurrentUser;

public interface CostControlService {

    /** Registra una merma: genera movimientos WASTE valorados al PMP. No puede dejar stock negativo. */
    WasteDTO registerWaste(WasteRequest request, CurrentUser requester);

    /** Filtros opcionales: null no filtra. Más recientes primero. */
    List<WasteDTO> getWaste(Instant from, Instant to, WasteReason reason);

    Optional<OrderStockPostingDTO> getOrderPosting(Long orderId);

    /** Consumo teórico frente a real entre dos recuentos cerrados del mismo almacén. */
    ConsumptionReportDTO consumptionReport(Long fromCountId, Long toCountId, CurrentUser requester);
}
