package com.barandgo.crm.purchasing.internal;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.purchasing.PurchaseOrderDTO;
import com.barandgo.crm.purchasing.PurchaseOrderRequestDTO;
import com.barandgo.crm.purchasing.PurchaseOrderService;
import com.barandgo.crm.purchasing.PurchaseOrderStatus;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @GetMapping
    public ResponseEntity<List<PurchaseOrderDTO>> getPurchaseOrders(
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) Long supplierId) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrders(status, supplierId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderDTO> getPurchaseOrder(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrder(id));
    }

    @PostMapping
    public ResponseEntity<PurchaseOrderDTO> createPurchaseOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PurchaseOrderRequestDTO request) {
        return new ResponseEntity<>(purchaseOrderService.createPurchaseOrder(request, CurrentUser.from(jwt)),
                HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PurchaseOrderDTO> updatePurchaseOrder(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderRequestDTO request) {
        return ResponseEntity.ok(purchaseOrderService.updatePurchaseOrder(id, request));
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<PurchaseOrderDTO> sendPurchaseOrder(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.sendPurchaseOrder(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PurchaseOrderDTO> cancelPurchaseOrder(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.cancelPurchaseOrder(id));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<PurchaseOrderDTO> closePurchaseOrder(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.closePurchaseOrder(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePurchaseOrder(@PathVariable Long id) {
        purchaseOrderService.deletePurchaseOrder(id);
        return ResponseEntity.noContent().build();
    }
}
