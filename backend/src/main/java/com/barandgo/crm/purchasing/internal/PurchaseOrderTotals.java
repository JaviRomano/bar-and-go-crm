package com.barandgo.crm.purchasing.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Importes de un pedido a proveedor. Sin dependencias: se prueba con valores directos.
 *
 * <ul>
 *   <li>Importe de línea: cantidad x precio, redondeado a céntimos.</li>
 *   <li>IVA: por tipo, sobre la suma de las bases de ese tipo (como en una factura), redondeado a
 *       céntimos. Calcularlo línea a línea y sumar puede dar céntimos de diferencia.</li>
 *   <li>Redondeo comercial (HALF_UP) en todos los casos.</li>
 * </ul>
 */
final class PurchaseOrderTotals {

    static final int MONEY_SCALE = 2;
    static final int PRICE_SCALE = 4;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    record Line(BigDecimal quantity, BigDecimal unitPrice, BigDecimal vatRate) {
    }

    record VatGroup(BigDecimal vatRate, BigDecimal base, BigDecimal vat) {
    }

    record Result(List<BigDecimal> lineAmounts, List<VatGroup> vatGroups,
                  BigDecimal totalBase, BigDecimal totalVat, BigDecimal total) {
    }

    private PurchaseOrderTotals() {
    }

    static BigDecimal lineAmount(BigDecimal quantity, BigDecimal unitPrice) {
        return quantity.multiply(unitPrice).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    static Result calculate(List<Line> lines) {
        List<BigDecimal> amounts = new ArrayList<>();
        // Tipos de mayor a menor; la escala fija evita que 21 y 21.00 se traten como tipos distintos
        Map<BigDecimal, BigDecimal> baseByRate = new TreeMap<>(Comparator.reverseOrder());

        for (Line line : lines) {
            BigDecimal amount = lineAmount(line.quantity(), line.unitPrice());
            amounts.add(amount);
            baseByRate.merge(line.vatRate().setScale(MONEY_SCALE, RoundingMode.HALF_UP), amount, BigDecimal::add);
        }

        List<VatGroup> groups = new ArrayList<>();
        BigDecimal totalBase = BigDecimal.ZERO.setScale(MONEY_SCALE);
        BigDecimal totalVat = BigDecimal.ZERO.setScale(MONEY_SCALE);
        for (Map.Entry<BigDecimal, BigDecimal> entry : baseByRate.entrySet()) {
            BigDecimal vat = entry.getValue().multiply(entry.getKey())
                    .divide(HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
            groups.add(new VatGroup(entry.getKey(), entry.getValue(), vat));
            totalBase = totalBase.add(entry.getValue());
            totalVat = totalVat.add(vat);
        }

        return new Result(amounts, groups, totalBase, totalVat, totalBase.add(totalVat));
    }

    /**
     * Precio en otra unidad a partir del precio de tarifa: si la tarifa es por caja y se pide por
     * unidad, precio por unidad = precio por caja x (cajas que hay en una unidad).
     *
     * @param tariffPrice              precio por unidad de tarifa
     * @param tariffUnitsPerOrderedUnit cuántas unidades de tarifa hay en una unidad pedida
     */
    static BigDecimal priceInOrderedUnit(BigDecimal tariffPrice, BigDecimal tariffUnitsPerOrderedUnit) {
        return tariffPrice.multiply(tariffUnitsPerOrderedUnit).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }
}
