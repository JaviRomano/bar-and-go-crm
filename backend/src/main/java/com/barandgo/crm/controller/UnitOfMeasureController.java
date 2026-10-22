package com.barandgo.crm.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.barandgo.crm.dto.unit.ConversionResultDTO;
import com.barandgo.crm.dto.unit.UnitConversionDTO;
import com.barandgo.crm.dto.unit.UnitOfMeasureDTO;
import com.barandgo.crm.service.UnitOfMeasureService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/units")
public class UnitOfMeasureController {

    private final UnitOfMeasureService unitService;

    public UnitOfMeasureController(UnitOfMeasureService unitService) {
        this.unitService = unitService;
    }

    @GetMapping
    public ResponseEntity<List<UnitOfMeasureDTO>> getUnits() {
        return ResponseEntity.ok(unitService.getUnits());
    }

    @PostMapping
    public ResponseEntity<UnitOfMeasureDTO> createUnit(@Valid @RequestBody UnitOfMeasureDTO unitDTO) {
        return new ResponseEntity<>(unitService.createUnit(unitDTO), HttpStatus.CREATED);
    }

    @GetMapping("/conversions")
    public ResponseEntity<List<UnitConversionDTO>> getConversions() {
        return ResponseEntity.ok(unitService.getConversions());
    }

    @PostMapping("/conversions")
    public ResponseEntity<UnitConversionDTO> createConversion(@Valid @RequestBody UnitConversionDTO conversionDTO) {
        return new ResponseEntity<>(unitService.createConversion(conversionDTO), HttpStatus.CREATED);
    }

    @DeleteMapping("/conversions/{id}")
    public ResponseEntity<Void> deleteConversion(@PathVariable Long id) {
        unitService.deleteConversion(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Convierte una cantidad con las reglas genéricas, p. ej. ?quantity=1.5&from=kg&to=g.
     */
    @GetMapping("/convert")
    public ResponseEntity<ConversionResultDTO> convert(
            @RequestParam BigDecimal quantity,
            @RequestParam String from,
            @RequestParam String to) {
        return ResponseEntity.ok(unitService.convert(quantity, from, to));
    }
}
