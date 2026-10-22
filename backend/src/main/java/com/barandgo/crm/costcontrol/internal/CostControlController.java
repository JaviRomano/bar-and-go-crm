package com.barandgo.crm.costcontrol.internal;

import java.time.Instant;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.barandgo.crm.costcontrol.CostControlDTOs.ConsumptionReportDTO;
import com.barandgo.crm.costcontrol.CostControlDTOs.OrderStockPostingDTO;
import com.barandgo.crm.costcontrol.CostControlDTOs.WasteDTO;
import com.barandgo.crm.costcontrol.CostControlDTOs.WasteRequest;
import com.barandgo.crm.costcontrol.CostControlService;
import com.barandgo.crm.costcontrol.WasteReason;
import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.shared.ResourceNotFoundException;

import jakarta.validation.Valid;

/**
 * Mermas (las registra el personal) y control de consumo (encargado).
 */
@RestController
public class CostControlController {

    private final CostControlService costControlService;

    public CostControlController(CostControlService costControlService) {
        this.costControlService = costControlService;
    }

    @PostMapping("/api/waste")
    public ResponseEntity<WasteDTO> registerWaste(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody WasteRequest request) {
        return new ResponseEntity<>(costControlService.registerWaste(request, CurrentUser.from(jwt)),
                HttpStatus.CREATED);
    }

    @GetMapping("/api/waste")
    public ResponseEntity<List<WasteDTO>> getWaste(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) WasteReason reason,
            @RequestParam(required = false) Long warehouseId) {
        return ResponseEntity.ok(costControlService.getWaste(from, to, reason, warehouseId));
    }

    @GetMapping("/api/cost-control/orders/{orderId}/stock-posting")
    public ResponseEntity<OrderStockPostingDTO> getOrderPosting(@PathVariable Long orderId) {
        return ResponseEntity.ok(costControlService.getOrderPosting(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("El pedido " + orderId + " no ha movido stock")));
    }

    @GetMapping("/api/cost-control/consumption")
    public ResponseEntity<ConsumptionReportDTO> getConsumption(@AuthenticationPrincipal Jwt jwt,
                                                               @RequestParam Long fromCountId,
                                                               @RequestParam Long toCountId) {
        return ResponseEntity.ok(costControlService.consumptionReport(fromCountId, toCountId, CurrentUser.from(jwt)));
    }
}
