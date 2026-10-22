package com.barandgo.crm.reporting.internal;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

import com.barandgo.crm.reporting.ReportDTOs.MenuClass;

/**
 * Matriz de ingeniería de menú (Kasavana y Smith): popularidad frente a margen de contribución.
 *
 * <ul>
 * <li>Popular: su cuota de ventas llega al 70 % de la cuota media (100 % / número de platos).</li>
 * <li>Rentable: su margen unitario llega al margen medio ponderado por unidades vendidas. Sin ventas, la
 * media simple.</li>
 * </ul>
 */
final class MenuEngineering {

    static final BigDecimal POPULARITY_FACTOR = new BigDecimal("0.70");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    record Input(long sold, BigDecimal unitMargin) {
    }

    record Thresholds(BigDecimal popularityPct, BigDecimal margin) {
    }

    private MenuEngineering() {
    }

    static Thresholds thresholds(List<Input> items) {
        if (items.isEmpty()) {
            return new Thresholds(BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal popularity = HUNDRED.multiply(POPULARITY_FACTOR)
                .divide(BigDecimal.valueOf(items.size()), 2, RoundingMode.HALF_UP);
        long totalSold = items.stream().mapToLong(Input::sold).sum();
        BigDecimal margin;
        if (totalSold > 0) {
            BigDecimal totalMargin = items.stream()
                    .map(item -> item.unitMargin().multiply(BigDecimal.valueOf(item.sold())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            margin = totalMargin.divide(BigDecimal.valueOf(totalSold), 2, RoundingMode.HALF_UP);
        } else {
            margin = items.stream().map(Input::unitMargin).reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(items.size()), 2, RoundingMode.HALF_UP);
        }
        return new Thresholds(popularity, margin);
    }

    static BigDecimal mixPct(long sold, long totalSold) {
        if (totalSold == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(sold).multiply(HUNDRED)
                .divide(BigDecimal.valueOf(totalSold), MathContext.DECIMAL64)
                .setScale(2, RoundingMode.HALF_UP);
    }

    static MenuClass classify(BigDecimal mixPct, BigDecimal unitMargin, Thresholds thresholds) {
        boolean popular = mixPct.signum() > 0 && mixPct.compareTo(thresholds.popularityPct()) >= 0;
        boolean profitable = unitMargin.compareTo(thresholds.margin()) >= 0;
        if (popular) {
            return profitable ? MenuClass.STAR : MenuClass.PLOWHORSE;
        }
        return profitable ? MenuClass.PUZZLE : MenuClass.DOG;
    }
}
