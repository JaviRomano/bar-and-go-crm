package com.barandgo.crm.inventory.internal;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.inventory.GoodsReceiptDTOs.GoodsReceiptDTO;
import com.barandgo.crm.inventory.GoodsReceiptDTOs.GoodsReceiptRequestDTO;
import com.barandgo.crm.inventory.GoodsReceiptService;

import jakarta.validation.Valid;

/**
 * Albaranes de recepción. Sin PUT ni DELETE: un albarán registrado ya ha movido stock.
 */
@RestController
@RequestMapping("/api/goods-receipts")
public class GoodsReceiptController {

    private final GoodsReceiptService goodsReceiptService;

    public GoodsReceiptController(GoodsReceiptService goodsReceiptService) {
        this.goodsReceiptService = goodsReceiptService;
    }

    @PostMapping
    public ResponseEntity<GoodsReceiptDTO> registerReceipt(@AuthenticationPrincipal Jwt jwt,
                                                           @Valid @RequestBody GoodsReceiptRequestDTO request) {
        return new ResponseEntity<>(goodsReceiptService.registerReceipt(request, CurrentUser.from(jwt)),
                HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<GoodsReceiptDTO>> getReceipts(@RequestParam(required = false) Long supplierId) {
        return ResponseEntity.ok(goodsReceiptService.getReceipts(supplierId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoodsReceiptDTO> getReceipt(@PathVariable Long id) {
        return ResponseEntity.ok(goodsReceiptService.getReceipt(id));
    }
}
