package com.barandgo.crm.reporting;

import java.time.Instant;
import java.time.LocalDate;

import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.reporting.ReportDTOs.FoodCostReport;
import com.barandgo.crm.reporting.ReportDTOs.MenuEngineeringReport;
import com.barandgo.crm.reporting.ReportDTOs.RotationReport;
import com.barandgo.crm.reporting.ReportDTOs.StockValueReport;
import com.barandgo.crm.reporting.ReportDTOs.SupplierPriceReport;
import com.barandgo.crm.reporting.ReportDTOs.WasteReport;

/**
 * Informes. Periodos: from incluido, to excluido. warehouseId null = todos los almacenes.
 */
public interface ReportService {

    /** Valor contable del stock a la fecha (PMP del ledger); at null = ahora. */
    StockValueReport stockValue(Instant at, Long warehouseId);

    FoodCostReport foodCost(Instant from, Instant to, Long warehouseId, CurrentUser requester);

    WasteReport waste(Instant from, Instant to, Long warehouseId);

    RotationReport rotation(Instant from, Instant to, Long warehouseId);

    /** Filtros opcionales por artículo y proveedor; fechas de compra incluidas las dos. */
    SupplierPriceReport supplierPrices(Long stockItemId, Long supplierId, LocalDate from, LocalDate to);

    MenuEngineeringReport menuEngineering(Instant from, Instant to, CurrentUser requester);
}
