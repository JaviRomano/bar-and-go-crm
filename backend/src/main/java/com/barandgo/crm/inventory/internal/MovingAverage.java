package com.barandgo.crm.inventory.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Precio medio ponderado (PMP) perpetuo, por artículo y local.
 *
 * <ul>
 *   <li>Una entrada valorada recalcula el PMP: (q_prev * pmp_prev + q_in * c_in) / (q_prev + q_in).</li>
 *   <li>Si el stock previo es cero o negativo, el PMP pasa a ser el coste de la entrada: promediar con
 *       una cantidad negativa daría precios absurdos (incluso negativos).</li>
 *   <li>Las salidas se valoran al PMP vigente y no lo cambian.</li>
 * </ul>
 */
final class MovingAverage {

    static final int COST_SCALE = 6;

    private MovingAverage() {
    }

    static BigDecimal afterReceipt(BigDecimal quantityBefore, BigDecimal averageBefore,
                                   BigDecimal quantityIn, BigDecimal unitCostIn) {
        if (quantityIn.signum() <= 0) {
            throw new IllegalArgumentException("Una entrada necesita cantidad positiva: " + quantityIn);
        }
        if (quantityBefore.signum() <= 0) {
            return unitCostIn.setScale(COST_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal value = quantityBefore.multiply(averageBefore).add(quantityIn.multiply(unitCostIn));
        return value.divide(quantityBefore.add(quantityIn), COST_SCALE, RoundingMode.HALF_UP);
    }
}
