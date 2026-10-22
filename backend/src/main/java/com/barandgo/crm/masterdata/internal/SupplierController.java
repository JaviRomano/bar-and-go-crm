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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.barandgo.crm.masterdata.SupplierDTO;
import com.barandgo.crm.masterdata.SupplierService;

import jakarta.validation.Valid;

/**
 * Proveedores. Sin DELETE: se desactivan con active=false en el PUT.
 */
@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public ResponseEntity<List<SupplierDTO>> getSuppliers(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(supplierService.getSuppliers(active));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierDTO> getSupplierById(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getSupplierById(id));
    }

    @PostMapping
    public ResponseEntity<SupplierDTO> createSupplier(@Valid @RequestBody SupplierDTO supplierDTO) {
        return new ResponseEntity<>(supplierService.createSupplier(supplierDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierDTO> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierDTO supplierDTO) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, supplierDTO));
    }
}
