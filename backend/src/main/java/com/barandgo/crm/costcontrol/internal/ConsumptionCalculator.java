package com.barandgo.crm.costcontrol.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.barandgo.crm.inventory.MovementType;

/**
 * Consumo teórico frente a real de un artículo en un periodo delimitado por dos recuentos (D15).
 *
 * <p>Si el stock inicial y final salieran del propio ledger, el consumo real coincidiría por construcción
 * con la suma de salidas registradas y la desviación sería siempre cero. Por eso se usan cantidades
 * contadas: lo que falta y nadie registró aparece como desviación.
 */
final class ConsumptionCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** Acumulado de movimientos de un artículo en el periodo, ya clasificado. */
    static final class Flows {
        private BigDecimal inflows = BigDecimal.ZERO;
        private BigDecimal transfersOut = BigDecimal.ZERO;
        private BigDecimal theoretical = BigDecimal.ZERO;
        private BigDecimal waste = BigDecimal.ZERO;

        /**
         * Clasifica un movimiento. Las anulaciones (cantidad de signo contrario) restan de su grupo. Los
         * ajustes no cuentan: el consumo real ya sale de los recuentos.
         */
        void add(MovementType type, BigDecimal quantity) {
            switch (type) {
                case PURCHASE_RECEIPT, PRODUCTION_OUTPUT -> inflows = inflows.add(quantity);
                case TRANSFER -> {
                    if (quantity.signum() > 0) {
                        inflows = inflows.add(quantity);
                    } else {
                        transfersOut = transfersOut.add(quantity.negate());
                    }
                }
                case SALE, PRODUCTION_CONSUMPTION -> theoretical = theoretical.subtract(quantity);
                case WASTE -> waste = waste.subtract(quantity);
                case ADJUSTMENT -> {
                    // Los recuentos ya miden el stock real; sumar sus ajustes contaría dos veces
                }
            }
        }

        BigDecimal inflows() {
            return inflows;
        }

        BigDecimal transfersOut() {
            return transfersOut;
        }

        BigDecimal theoretical() {
            return theoretical;
        }

        BigDecimal waste() {
            return waste;
        }
    }

    record Result(BigDecimal real, BigDecimal deviation, BigDecimal deviationPct, BigDecimal unexplained) {
    }

    private ConsumptionCalculator() {
    }

    static Result calculate(BigDecimal opening, BigDecimal closing, Flows flows) {
        BigDecimal real = opening.add(flows.inflows()).subtract(flows.transfersOut()).subtract(closing);
        BigDecimal deviation = real.subtract(flows.theoretical());
        BigDecimal pct = flows.theoretical().signum() == 0 ? null
                : deviation.multiply(HUNDRED).divide(flows.theoretical(), 2, RoundingMode.HALF_UP);
        return new Result(real, deviation, pct, deviation.subtract(flows.waste()));
    }
}
