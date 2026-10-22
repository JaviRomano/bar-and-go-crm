package com.barandgo.crm.purchasing.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.barandgo.crm.purchasing.internal.PurchaseOrderTotals.Line;
import com.barandgo.crm.purchasing.internal.PurchaseOrderTotals.Result;

class PurchaseOrderTotalsTest {

    @Test
    void lineAmountIsRoundedHalfUpToCents() {
        assertThat(PurchaseOrderTotals.lineAmount(new BigDecimal("3"), new BigDecimal("1.235")))
                .isEqualByComparingTo("3.71");
        assertThat(PurchaseOrderTotals.lineAmount(new BigDecimal("2.5"), new BigDecimal("0.333")))
                .isEqualByComparingTo("0.83");
    }

    @Test
    void groupsBaseAndVatByRateHighestFirst() {
        Result result = PurchaseOrderTotals.calculate(List.of(
                line("2", "10.00", "21"),
                line("1", "5.50", "21"),
                line("4", "2.25", "10")));

        assertThat(result.vatGroups()).hasSize(2);
        assertThat(result.vatGroups().get(0).vatRate()).isEqualByComparingTo("21");
        assertThat(result.vatGroups().get(0).base()).isEqualByComparingTo("25.50");
        assertThat(result.vatGroups().get(0).vat()).isEqualByComparingTo("5.36");  // 5.355
        assertThat(result.vatGroups().get(1).vatRate()).isEqualByComparingTo("10");
        assertThat(result.vatGroups().get(1).base()).isEqualByComparingTo("9.00");
        assertThat(result.vatGroups().get(1).vat()).isEqualByComparingTo("0.90");

        assertThat(result.totalBase()).isEqualByComparingTo("34.50");
        assertThat(result.totalVat()).isEqualByComparingTo("6.26");
        assertThat(result.total()).isEqualByComparingTo("40.76");
        assertThat(result.lineAmounts()).containsExactly(
                new BigDecimal("20.00"), new BigDecimal("5.50"), new BigDecimal("9.00"));
    }

    @Test
    void vatIsRoundedOnTheGroupNotOnEachLine() {
        // Tres líneas de 0,05 € al 10 %: línea a línea serían 3 x 0,01 = 0,03; sobre la base agrupada, 0,02
        Result result = PurchaseOrderTotals.calculate(List.of(
                line("1", "0.05", "10"),
                line("1", "0.05", "10"),
                line("1", "0.05", "10")));

        assertThat(result.totalVat()).isEqualByComparingTo("0.02");
        assertThat(result.total()).isEqualByComparingTo("0.17");
    }

    @Test
    void sameRateWithDifferentScaleIsOneGroup() {
        Result result = PurchaseOrderTotals.calculate(List.of(
                line("1", "10", "21"),
                line("1", "10", "21.00")));

        assertThat(result.vatGroups()).singleElement()
                .satisfies(group -> assertThat(group.base()).isEqualByComparingTo("20.00"));
    }

    @Test
    void zeroRateProducesNoVat() {
        Result result = PurchaseOrderTotals.calculate(List.of(line("3", "1.50", "0")));

        assertThat(result.totalVat()).isEqualByComparingTo("0");
        assertThat(result.total()).isEqualByComparingTo("4.50");
    }

    @Test
    void emptyOrderTotalsAreZero() {
        Result result = PurchaseOrderTotals.calculate(List.of());

        assertThat(result.vatGroups()).isEmpty();
        assertThat(result.total()).isEqualByComparingTo("0");
        assertThat(result.total().scale()).isEqualTo(2);
    }

    @Test
    void tariffPerBoxConvertedToPricePerUnit() {
        // Tarifa: 12 € la caja de 24. Una unidad es 1/24 de caja
        BigDecimal boxesPerUnit = BigDecimal.ONE.divide(new BigDecimal("24"), MathContext.DECIMAL128);

        assertThat(PurchaseOrderTotals.priceInOrderedUnit(new BigDecimal("12.00"), boxesPerUnit))
                .isEqualByComparingTo("0.5000");
    }

    @Test
    void tariffPerUnitConvertedToPricePerBox() {
        assertThat(PurchaseOrderTotals.priceInOrderedUnit(new BigDecimal("0.75"), new BigDecimal("24")))
                .isEqualByComparingTo("18.0000");
    }

    @Test
    void convertedPriceKeepsFourDecimals() {
        BigDecimal third = BigDecimal.ONE.divide(new BigDecimal("3"), MathContext.DECIMAL128);

        BigDecimal price = PurchaseOrderTotals.priceInOrderedUnit(new BigDecimal("1.00"), third);

        assertThat(price).isEqualByComparingTo("0.3333");
        assertThat(price.scale()).isEqualTo(4);
    }

    private static Line line(String quantity, String unitPrice, String vatRate) {
        return new Line(new BigDecimal(quantity), new BigDecimal(unitPrice), new BigDecimal(vatRate));
    }
}
