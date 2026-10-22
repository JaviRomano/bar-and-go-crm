package com.barandgo.crm.masterdata.internal;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.barandgo.crm.masterdata.VatRateDTO;
import com.barandgo.crm.masterdata.VatRateService;

import jakarta.validation.Valid;

/**
 * Tipos de IVA. Sin DELETE: se desactivan con active=false en el PUT.
 */
@RestController
@RequestMapping("/api/vat-rates")
public class VatRateController {

    private final VatRateService vatRateService;

    public VatRateController(VatRateService vatRateService) {
        this.vatRateService = vatRateService;
    }

    @GetMapping
    public ResponseEntity<List<VatRateDTO>> getVatRates() {
        return ResponseEntity.ok(vatRateService.getVatRates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VatRateDTO> getVatRate(@PathVariable Long id) {
        return ResponseEntity.ok(vatRateService.getVatRate(id));
    }

    @PostMapping
    public ResponseEntity<VatRateDTO> createVatRate(@Valid @RequestBody VatRateDTO vatRateDTO) {
        return new ResponseEntity<>(vatRateService.createVatRate(vatRateDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VatRateDTO> updateVatRate(@PathVariable Long id, @Valid @RequestBody VatRateDTO vatRateDTO) {
        return ResponseEntity.ok(vatRateService.updateVatRate(id, vatRateDTO));
    }
}
