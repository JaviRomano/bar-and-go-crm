package com.barandgo.crm.reporting.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.barandgo.crm.reporting.ReportDTOs.MenuClass;
import com.barandgo.crm.reporting.internal.MenuEngineering.Input;
import com.barandgo.crm.reporting.internal.MenuEngineering.Thresholds;

class ReportCalculationsTest {

    @Nested
    class Menu {

        @Test
        void thresholdsUseSeventyPercentOfAverageMixAndWeightedMargin() {
            // 4 platos: cuota media 25 %, umbral 17,5 %. Margen ponderado: (60x5 + 30x3 + 8x8 + 2x4) / 100
            Thresholds thresholds = MenuEngineering.thresholds(List.of(
                    new Input(60, dec("5")), new Input(30, dec("3")), new Input(8, dec("8")), new Input(2, dec("4"))));

            assertThat(thresholds.popularityPct()).isEqualByComparingTo("17.50");
            assertThat(thresholds.margin()).isEqualByComparingTo("4.62");
        }

        @Test
        void classifiesTheFourQuadrants() {
            Thresholds thresholds = new Thresholds(dec("17.50"), dec("4.62"));

            assertThat(MenuEngineering.classify(dec("60"), dec("5"), thresholds)).isEqualTo(MenuClass.STAR);
            assertThat(MenuEngineering.classify(dec("30"), dec("3"), thresholds)).isEqualTo(MenuClass.PLOWHORSE);
            assertThat(MenuEngineering.classify(dec("8"), dec("8"), thresholds)).isEqualTo(MenuClass.PUZZLE);
            assertThat(MenuEngineering.classify(dec("2"), dec("4"), thresholds)).isEqualTo(MenuClass.DOG);
        }

        @Test
        void exactlyOnTheThresholdCountsAsHigh() {
            Thresholds thresholds = new Thresholds(dec("17.50"), dec("4.62"));

            assertThat(MenuEngineering.classify(dec("17.50"), dec("4.62"), thresholds)).isEqualTo(MenuClass.STAR);
        }

        @Test
        void withoutSalesMarginThresholdIsTheSimpleAverageAndNothingIsPopular() {
            Thresholds thresholds = MenuEngineering.thresholds(List.of(new Input(0, dec("4")), new Input(0, dec("6"))));

            assertThat(thresholds.margin()).isEqualByComparingTo("5.00");
            assertThat(MenuEngineering.mixPct(0, 0)).isEqualByComparingTo("0");
            assertThat(MenuEngineering.classify(dec("0"), dec("6"), thresholds)).isEqualTo(MenuClass.PUZZLE);
        }

        @Test
        void mixIsShareOfUnitsSold() {
            assertThat(MenuEngineering.mixPct(1, 3)).isEqualByComparingTo("33.33");
        }
    }

    @Nested
    class Math {

        @Test
        void netRemovesVat() {
            assertThat(ReportMath.net(dec("11.00"), dec("10"))).isEqualByComparingTo("10");
            assertThat(ReportMath.net(dec("5.00"), null)).isEqualByComparingTo("5");
        }

        @Test
        void percentageOfZeroIsUndefined() {
            assertThat(ReportMath.pct(dec("3"), BigDecimal.ZERO)).isNull();
            assertThat(ReportMath.pct(dec("3"), dec("10"))).isEqualByComparingTo("30.00");
        }

        @Test
        void rotationOverAverageStock() {
            // Stock medio 150, consumo 600 en 30 días: rota 4 veces, el stock dura 7,5 días
            ReportMath.Rotation rotation = ReportMath.rotation(dec("100"), dec("200"), dec("600"), 30);

            assertThat(rotation.averageValue()).isEqualByComparingTo("150.00");
            assertThat(rotation.rotation()).isEqualByComparingTo("4.00");
            assertThat(rotation.coverageDays()).isEqualByComparingTo("7.5");
        }

        @Test
        void stockWithoutConsumptionHasNoCoverage() {
            ReportMath.Rotation rotation = ReportMath.rotation(dec("80"), dec("80"), BigDecimal.ZERO, 30);

            assertThat(rotation.rotation()).isEqualByComparingTo("0");
            assertThat(rotation.coverageDays()).isNull();
        }

        @Test
        void consumedEverythingWithoutAverageStock() {
            ReportMath.Rotation rotation = ReportMath.rotation(BigDecimal.ZERO, BigDecimal.ZERO, dec("50"), 30);

            assertThat(rotation.rotation()).isNull();
        }
    }

    private static BigDecimal dec(String value) {
        return new BigDecimal(value);
    }
}
