package com.barandgo.crm.reporting;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.barandgo.crm.costcontrol.WasteReason;

/**
 * Respuestas de los informes. Importes en euros sin IVA salvo que el nombre diga lo contrario; cantidades
 * en unidad de inventario; porcentajes con dos decimales.
 */
public final class ReportDTOs {

    private ReportDTOs() {
    }

    // ---- Valor de stock ----

    public record StockValueReport(
            Instant at,
            Long warehouseId,
            List<StockValueLine> lines,
            List<GroupValue> byCategory,
            List<GroupValue> byWarehouse,
            BigDecimal totalValue) {
    }

    public record StockValueLine(
            Long stockItemId,
            String stockItemCode,
            String stockItemName,
            String category,
            String unit,
            BigDecimal quantity,
            BigDecimal averageCost,
            BigDecimal value) {
    }

    public record GroupValue(Long id, String name, BigDecimal value, BigDecimal sharePct) {
    }

    // ---- Food cost ----

    /**
     * Teórico: coste de lo que dicen los escandallos de lo vendido (salidas SALE). Real: todo lo que ha
     * salido del stock por consumo (ventas, mermas, ajustes y diferencias de recuento, producción neta),
     * equivalente a stock inicial + compras - stock final.
     */
    public record FoodCostReport(
            Instant from,
            Instant to,
            Long warehouseId,
            BigDecimal grossRevenue,
            BigDecimal netRevenue,
            BigDecimal revenueWithoutRecipe,
            BigDecimal theoreticalCost,
            BigDecimal wasteCost,
            BigDecimal adjustmentCost,
            BigDecimal productionVariance,
            BigDecimal realCost,
            BigDecimal theoreticalPct,
            BigDecimal realPct,
            BigDecimal deviationPts,
            String bucket,
            List<FoodCostPoint> series) {
    }

    public record FoodCostPoint(
            Instant start,
            BigDecimal netRevenue,
            BigDecimal theoreticalCost,
            BigDecimal realCost,
            BigDecimal theoreticalPct,
            BigDecimal realPct) {
    }

    // ---- Mermas ----

    public record WasteReport(
            Instant from,
            Instant to,
            Long warehouseId,
            BigDecimal totalValue,
            List<WasteByReason> byReason,
            List<WasteBySubject> bySubject,
            List<WasteByItem> byItem) {
    }

    public record WasteByReason(WasteReason reason, long count, BigDecimal value, BigDecimal sharePct) {
    }

    /** Lo que se registró como merma: un artículo o un plato. */
    public record WasteBySubject(
            String kind,
            Long id,
            String name,
            long count,
            BigDecimal quantity,
            String unit,
            BigDecimal value) {
    }

    /** Lo que salió del stock por mermas, con los platos ya descompuestos en artículos. */
    public record WasteByItem(
            Long stockItemId,
            String stockItemCode,
            String stockItemName,
            String unit,
            BigDecimal quantity,
            BigDecimal value) {
    }

    // ---- Rotación ----

    public record RotationReport(
            Instant from,
            Instant to,
            Long warehouseId,
            long days,
            List<RotationLine> lines,
            BigDecimal totalConsumption,
            BigDecimal totalAverageValue,
            BigDecimal rotation,
            BigDecimal coverageDays) {
    }

    /**
     * rotation = consumo / stock medio (veces en el periodo). coverageDays = días que dura el stock medio
     * al ritmo de consumo del periodo; null si no hubo consumo.
     */
    public record RotationLine(
            Long stockItemId,
            String stockItemCode,
            String stockItemName,
            String category,
            BigDecimal openingValue,
            BigDecimal closingValue,
            BigDecimal averageValue,
            BigDecimal consumptionValue,
            BigDecimal rotation,
            BigDecimal coverageDays) {
    }

    // ---- Precios de compra ----

    public record SupplierPriceReport(LocalDate from, LocalDate to, List<PriceSeries> series) {
    }

    /** Precios pagados de un artículo a un proveedor, por unidad de inventario. */
    public record PriceSeries(
            Long stockItemId,
            String stockItemCode,
            String stockItemName,
            Long supplierId,
            String supplierName,
            String unit,
            List<PricePoint> points,
            BigDecimal firstPrice,
            BigDecimal lastPrice,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal changePct) {
    }

    public record PricePoint(LocalDate date, BigDecimal unitPrice, String reference) {
    }

    // ---- Ingeniería de menú ----

    public enum MenuClass {
        /** Popular y rentable: mantener. */
        STAR,
        /** Popular y poco rentable ("vaca"): revisar precio o escandallo. */
        PLOWHORSE,
        /** Rentable y poco vendido: darle visibilidad. */
        PUZZLE,
        /** Ni lo uno ni lo otro ("perro"): candidato a salir de la carta. */
        DOG
    }

    public record MenuEngineeringReport(
            Instant from,
            Instant to,
            long totalSold,
            BigDecimal popularityThresholdPct,
            BigDecimal marginThreshold,
            List<MenuItem> items,
            List<String> productsWithoutRecipe) {
    }

    public record MenuItem(
            Long productId,
            String productName,
            String category,
            long sold,
            BigDecimal mixPct,
            BigDecimal netPrice,
            BigDecimal costPerPortion,
            Boolean costComplete,
            BigDecimal unitMargin,
            BigDecimal totalMargin,
            BigDecimal foodCostPct,
            MenuClass classification) {
    }
}
