package com.barandgo.crm.purchasing.internal;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.barandgo.crm.purchasing.PurchasePriceDTO;
import com.barandgo.crm.purchasing.StockItemDTO;
import com.barandgo.crm.purchasing.StockItemService;
import com.barandgo.crm.purchasing.StockItemSupplierDTO;

import jakarta.validation.Valid;

/**
 * Artículos de compra. Sin DELETE: se desactivan con active=false en el PUT.
 * Los precios por proveedor cuelgan de /{id}/suppliers.
 */
@RestController
@RequestMapping("/api/stock-items")
public class StockItemController {

    private final StockItemService stockItemService;

    public StockItemController(StockItemService stockItemService) {
        this.stockItemService = stockItemService;
    }

    @GetMapping
    public ResponseEntity<List<StockItemDTO>> getStockItems(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(stockItemService.getStockItems(active, categoryId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockItemDTO> getStockItem(@PathVariable Long id) {
        return ResponseEntity.ok(stockItemService.getStockItem(id));
    }

    @PostMapping
    public ResponseEntity<StockItemDTO> createStockItem(@Valid @RequestBody StockItemDTO stockItemDTO) {
        return new ResponseEntity<>(stockItemService.createStockItem(stockItemDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StockItemDTO> updateStockItem(
            @PathVariable Long id,
            @Valid @RequestBody StockItemDTO stockItemDTO) {
        return ResponseEntity.ok(stockItemService.updateStockItem(id, stockItemDTO));
    }

    @GetMapping("/{id}/suppliers")
    public ResponseEntity<List<StockItemSupplierDTO>> getSuppliers(@PathVariable Long id) {
        return ResponseEntity.ok(stockItemService.getSuppliers(id));
    }

    @PutMapping("/{id}/suppliers/{supplierId}")
    public ResponseEntity<StockItemSupplierDTO> setSupplierPrice(
            @PathVariable Long id,
            @PathVariable Long supplierId,
            @Valid @RequestBody StockItemSupplierDTO priceDTO) {
        return ResponseEntity.ok(stockItemService.setSupplierPrice(id, supplierId, priceDTO));
    }

    @GetMapping("/{id}/price-history")
    public ResponseEntity<List<PurchasePriceDTO>> getPriceHistory(@PathVariable Long id) {
        return ResponseEntity.ok(stockItemService.getPriceHistory(id));
    }

    @DeleteMapping("/{id}/suppliers/{supplierId}")
    public ResponseEntity<Void> removeSupplier(@PathVariable Long id, @PathVariable Long supplierId) {
        stockItemService.removeSupplier(id, supplierId);
        return ResponseEntity.noContent().build();
    }
}
