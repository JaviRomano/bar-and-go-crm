package com.barandgo.crm.inventory.internal;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.inventory.StockCountDTOs.CountEntriesRequest;
import com.barandgo.crm.inventory.StockCountDTOs.OpenCountRequest;
import com.barandgo.crm.inventory.StockCountDTOs.StockCountDTO;
import com.barandgo.crm.inventory.StockCountService;

import jakarta.validation.Valid;

/**
 * Recuentos: el encargado abre y cierra; el personal registra cantidades contadas.
 */
@RestController
@RequestMapping("/api/stock-counts")
public class StockCountController {

    private final StockCountService stockCountService;

    public StockCountController(StockCountService stockCountService) {
        this.stockCountService = stockCountService;
    }

    @PostMapping
    public ResponseEntity<StockCountDTO> openCount(@AuthenticationPrincipal Jwt jwt,
                                                   @Valid @RequestBody OpenCountRequest request) {
        return new ResponseEntity<>(stockCountService.openCount(request, CurrentUser.from(jwt)), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<StockCountDTO>> getCounts(@AuthenticationPrincipal Jwt jwt,
                                                         @RequestParam(required = false) Long warehouseId) {
        return ResponseEntity.ok(stockCountService.getCounts(warehouseId, CurrentUser.from(jwt)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockCountDTO> getCount(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ResponseEntity.ok(stockCountService.getCount(id, CurrentUser.from(jwt)));
    }

    @PutMapping("/{id}/lines")
    public ResponseEntity<StockCountDTO> recordCounts(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                                      @Valid @RequestBody CountEntriesRequest request) {
        return ResponseEntity.ok(stockCountService.recordCounts(id, request, CurrentUser.from(jwt)));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<StockCountDTO> closeCount(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ResponseEntity.ok(stockCountService.closeCount(id, CurrentUser.from(jwt)));
    }
}
