package com.barandgo.crm.inventory.internal;

import java.time.Instant;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.inventory.InventoryDTOs.AdjustmentRequestDTO;
import com.barandgo.crm.inventory.InventoryDTOs.ExpiringLotDTO;
import com.barandgo.crm.inventory.InventoryDTOs.StockBalanceDTO;
import com.barandgo.crm.inventory.InventoryDTOs.StockMovementDTO;
import com.barandgo.crm.inventory.InventoryDTOs.TransferRequestDTO;
import com.barandgo.crm.inventory.InventoryDTOs.ValuationReportDTO;
import com.barandgo.crm.inventory.InventoryService;
import com.barandgo.crm.inventory.ReplenishmentDTOs.CreateOrdersRequest;
import com.barandgo.crm.inventory.ReplenishmentDTOs.SupplierGroup;
import com.barandgo.crm.inventory.ReplenishmentService;
import com.barandgo.crm.purchasing.PurchaseOrderDTO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final ReplenishmentService replenishmentService;

    public InventoryController(InventoryService inventoryService, ReplenishmentService replenishmentService) {
        this.inventoryService = inventoryService;
        this.replenishmentService = replenishmentService;
    }

    @GetMapping("/stock")
    public ResponseEntity<List<StockBalanceDTO>> getStock(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long stockItemId) {
        return ResponseEntity.ok(inventoryService.getStock(warehouseId, stockItemId));
    }

    @GetMapping("/valuation")
    public ResponseEntity<ValuationReportDTO> getValuation() {
        return ResponseEntity.ok(inventoryService.getValuation());
    }

    /**
     * Movimientos, más recientes primero. from y to en ISO-8601 (2026-10-01T00:00:00Z); to es exclusivo.
     */
    @GetMapping("/movements")
    public ResponseEntity<List<StockMovementDTO>> getMovements(
            @RequestParam(required = false) Long stockItemId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(inventoryService.getMovements(stockItemId, warehouseId, from, to));
    }

    @PostMapping("/adjustments")
    public ResponseEntity<List<StockMovementDTO>> adjust(@AuthenticationPrincipal Jwt jwt,
                                                         @Valid @RequestBody AdjustmentRequestDTO request) {
        return new ResponseEntity<>(inventoryService.adjust(request, CurrentUser.from(jwt)), HttpStatus.CREATED);
    }

    @PostMapping("/transfers")
    public ResponseEntity<List<StockMovementDTO>> transfer(@AuthenticationPrincipal Jwt jwt,
                                                           @Valid @RequestBody TransferRequestDTO request) {
        return new ResponseEntity<>(inventoryService.transfer(request, CurrentUser.from(jwt)), HttpStatus.CREATED);
    }

    @GetMapping("/lots/expiring")
    public ResponseEntity<List<ExpiringLotDTO>> getExpiringLots(@RequestParam(defaultValue = "3") int days) {
        return ResponseEntity.ok(inventoryService.getExpiringLots(days));
    }

    @GetMapping("/replenishment")
    public ResponseEntity<List<SupplierGroup>> getReplenishment() {
        return ResponseEntity.ok(replenishmentService.getSuggestions());
    }

    @PostMapping("/replenishment/orders")
    public ResponseEntity<List<PurchaseOrderDTO>> createReplenishmentOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) CreateOrdersRequest request) {
        return new ResponseEntity<>(replenishmentService.createDraftOrders(request, CurrentUser.from(jwt)),
                HttpStatus.CREATED);
    }
}
