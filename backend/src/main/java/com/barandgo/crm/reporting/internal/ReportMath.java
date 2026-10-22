package com.barandgo.crm.reporting.internal;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Operaciones comunes a los informes: porcentajes, importes sin IVA y rotación de stock.
 */
final class ReportMath {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    record Rotation(BigDecimal averageValue, BigDecimal rotation, BigDecimal coverageDays) {
    }

    private ReportMath() {
    }

    static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /** part / whole en %, o null si whole es cero. */
    static BigDecimal pct(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() == 0) {
            return null;
        }
        return part.multiply(HUNDRED).divide(whole, 2, RoundingMode.HALF_UP);
    }

    /** Importe sin IVA a partir de uno con IVA y el tipo en %. */
    static BigDecimal net(BigDecimal gross, BigDecimal vatRatePct) {
        BigDecimal rate = vatRatePct == null ? BigDecimal.ZERO : vatRatePct;
        return gross.multiply(HUNDRED).divide(HUNDRED.add(rate), MathContext.DECIMAL64);
    }

    /**
     * Rotación por valor: consumo del periodo / stock medio, con stock medio = (inicial + final) / 2. La
     * cobertura son los días que duraría el stock medio a ese ritmo.
     */
    static Rotation rotation(BigDecimal openingValue, BigDecimal closingValue, BigDecimal consumption, long days) {
        BigDecimal average = openingValue.add(closingValue).divide(TWO, MathContext.DECIMAL64);
        if (average.signum() <= 0) {
            return new Rotation(money(average.max(BigDecimal.ZERO)), null, null);
        }
        BigDecimal rotation = consumption.divide(average, MathContext.DECIMAL64);
        BigDecimal coverage = rotation.signum() <= 0 ? null
                : BigDecimal.valueOf(days).divide(rotation, 1, RoundingMode.HALF_UP);
        return new Rotation(money(average), rotation.setScale(2, RoundingMode.HALF_UP), coverage);
    }
}
