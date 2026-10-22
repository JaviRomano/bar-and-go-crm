package com.barandgo.crm.reporting.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.reporting.ReportDTOs.FoodCostReport;
import com.barandgo.crm.reporting.ReportDTOs.MenuEngineeringReport;
import com.barandgo.crm.reporting.ReportDTOs.RotationReport;
import com.barandgo.crm.reporting.ReportDTOs.StockValueReport;
import com.barandgo.crm.reporting.ReportDTOs.SupplierPriceReport;
import com.barandgo.crm.reporting.ReportDTOs.WasteReport;
import com.barandgo.crm.reporting.ReportService;

/**
 * Informes de gestión. Las fechas son días (yyyy-MM-dd) en la zona del local, los dos incluidos. Sin
 * fechas, los últimos 30 días.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final int DEFAULT_DAYS = 30;

    private final ReportService reportService;
    private final ZoneId zone;

    public ReportController(ReportService reportService, @Value("${app.reports.zone}") ZoneId zone) {
        this.reportService = reportService;
        this.zone = zone;
    }

    /** Periodo [from 00:00, to + 1 día 00:00) en la zona del local. */
    private record Period(Instant from, Instant to) {
    }

    private Period period(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now(zone);
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_DAYS - 1);
        return new Period(start.atStartOfDay(zone).toInstant(), end.plusDays(1).atStartOfDay(zone).toInstant());
    }

    @GetMapping("/stock-value")
    public ResponseEntity<StockValueReport> stockValue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate at,
            @RequestParam(required = false) Long warehouseId) {
        Instant when = at == null ? null : at.plusDays(1).atStartOfDay(zone).toInstant();
        return ResponseEntity.ok(reportService.stockValue(when, warehouseId));
    }

    @GetMapping("/food-cost")
    public ResponseEntity<FoodCostReport> foodCost(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long warehouseId) {
        Period period = period(from, to);
        return ResponseEntity.ok(reportService.foodCost(period.from(), period.to(), warehouseId,
                CurrentUser.from(jwt)));
    }

    @GetMapping("/waste")
    public ResponseEntity<WasteReport> waste(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long warehouseId) {
        Period period = period(from, to);
        return ResponseEntity.ok(reportService.waste(period.from(), period.to(), warehouseId));
    }

    @GetMapping("/rotation")
    public ResponseEntity<RotationReport> rotation(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long warehouseId) {
        Period period = period(from, to);
        return ResponseEntity.ok(reportService.rotation(period.from(), period.to(), warehouseId));
    }

    @GetMapping("/supplier-prices")
    public ResponseEntity<SupplierPriceReport> supplierPrices(
            @RequestParam(required = false) Long stockItemId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.supplierPrices(stockItemId, supplierId, from, to));
    }

    @GetMapping("/menu-engineering")
    public ResponseEntity<MenuEngineeringReport> menuEngineering(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Period period = period(from, to);
        return ResponseEntity.ok(reportService.menuEngineering(period.from(), period.to(), CurrentUser.from(jwt)));
    }
}
