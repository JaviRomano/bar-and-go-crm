package com.barandgo.crm.recipes.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Indicadores de precio de un plato. El PVP de la carta incluye IVA; food cost y margen se calculan
 * sobre el precio sin IVA, que es lo que ingresa el local.
 */
final class MenuPricing {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private MenuPricing() {
    }

    static BigDecimal netPrice(BigDecimal priceWithVat, BigDecimal vatRate) {
        return priceWithVat.multiply(HUNDRED).divide(HUNDRED.add(vatRate), 4, RoundingMode.HALF_UP);
    }

    /** Coste por ración sobre PVP sin IVA, en %. Null si no hay precio. */
    static BigDecimal foodCostPct(BigDecimal costPerPortion, BigDecimal priceWithVat, BigDecimal vatRate) {
        BigDecimal net = netPrice(priceWithVat, vatRate);
        if (net.signum() == 0) {
            return null;
        }
        return costPerPortion.multiply(HUNDRED).divide(net, 2, RoundingMode.HALF_UP);
    }

    /** Margen bruto por ración: PVP sin IVA menos coste. */
    static BigDecimal grossMargin(BigDecimal costPerPortion, BigDecimal priceWithVat, BigDecimal vatRate) {
        return netPrice(priceWithVat, vatRate).subtract(costPerPortion).setScale(2, RoundingMode.HALF_UP);
    }

    /** PVP con IVA para alcanzar el food cost objetivo: coste / objetivo, más IVA. */
    static BigDecimal suggestedPrice(BigDecimal costPerPortion, BigDecimal targetFoodCostPct, BigDecimal vatRate) {
        return costPerPortion.multiply(HUNDRED.add(vatRate))
                .divide(targetFoodCostPct, 2, RoundingMode.HALF_UP);
    }
}
