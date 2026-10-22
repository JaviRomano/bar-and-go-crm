package com.barandgo.crm.costcontrol.internal;

import static com.barandgo.crm.inventory.MovementType.ADJUSTMENT;
import static com.barandgo.crm.inventory.MovementType.PRODUCTION_CONSUMPTION;
import static com.barandgo.crm.inventory.MovementType.PRODUCTION_OUTPUT;
import static com.barandgo.crm.inventory.MovementType.PURCHASE_RECEIPT;
import static com.barandgo.crm.inventory.MovementType.SALE;
import static com.barandgo.crm.inventory.MovementType.TRANSFER;
import static com.barandgo.crm.inventory.MovementType.WASTE;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.barandgo.crm.costcontrol.internal.ConsumptionCalculator.Flows;
import com.barandgo.crm.costcontrol.internal.ConsumptionCalculator.Result;

class ConsumptionCalculatorTest {

    @Test
    void realConsumptionComesFromCountsAndInflows() {
        // 10 contados + 24 comprados - 4 subidos a barra - 12 contados al final = 18 consumidos
        Flows flows = new Flows();
        flows.add(PURCHASE_RECEIPT, dec("24"));
        flows.add(TRANSFER, dec("-4"));
        flows.add(SALE, dec("-15"));

        Result result = ConsumptionCalculator.calculate(dec("10"), dec("12"), flows);

        assertThat(result.real()).isEqualByComparingTo("18");
        assertThat(flows.theoretical()).isEqualByComparingTo("15");
        assertThat(result.deviation()).isEqualByComparingTo("3");
        assertThat(result.deviationPct()).isEqualByComparingTo("20.00");
    }

    @Test
    void recordedWasteExplainsPartOfTheDeviation() {
        Flows flows = new Flows();
        flows.add(PURCHASE_RECEIPT, dec("20"));
        flows.add(SALE, dec("-12"));
        flows.add(WASTE, dec("-2"));

        Result result = ConsumptionCalculator.calculate(BigDecimal.ZERO, dec("5"), flows);

        // real 15, teórico 12: 3 de desviación, 2 son merma registrada, 1 sin explicar
        assertThat(result.deviation()).isEqualByComparingTo("3");
        assertThat(flows.waste()).isEqualByComparingTo("2");
        assertThat(result.unexplained()).isEqualByComparingTo("1");
    }

    @Test
    void cancelledSaleDoesNotCountAsTheoretical() {
        Flows flows = new Flows();
        flows.add(SALE, dec("-3"));
        flows.add(SALE, dec("3"));  // compensación de la venta cancelada

        assertThat(flows.theoretical()).isEqualByComparingTo("0");
    }

    @Test
    void productionCountsAsTheoreticalForIngredientsAndAsInflowForTheResult() {
        Flows tomato = new Flows();
        tomato.add(PRODUCTION_CONSUMPTION, dec("-1.1"));
        Flows sauce = new Flows();
        sauce.add(PRODUCTION_OUTPUT, dec("1"));

        assertThat(tomato.theoretical()).isEqualByComparingTo("1.1");
        assertThat(sauce.inflows()).isEqualByComparingTo("1");
    }

    @Test
    void adjustmentsAreIgnoredBecauseCountsAlreadyMeasureStock() {
        Flows flows = new Flows();
        flows.add(ADJUSTMENT, dec("-5"));

        Result result = ConsumptionCalculator.calculate(dec("10"), dec("10"), flows);

        assertThat(result.real()).isEqualByComparingTo("0");
    }

    @Test
    void withoutTheoreticalConsumptionThereIsNoPercentage() {
        Result result = ConsumptionCalculator.calculate(dec("5"), dec("4"), new Flows());

        assertThat(result.real()).isEqualByComparingTo("1");
        assertThat(result.deviationPct()).isNull();
    }

    private static BigDecimal dec(String value) {
        return new BigDecimal(value);
    }
}
