package com.barandgo.crm.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.barandgo.crm.inventory.internal.LotAllocator.Allocation;
import com.barandgo.crm.inventory.internal.LotAllocator.Lot;

class InventoryCalculationsTest {

    @Nested
    class MovingAverageCost {

        @Test
        void weightsPreviousStockAndNewEntry() {
            // 24 ud a 0,30 + 24 ud a 0,40 -> 0,35
            assertThat(MovingAverage.afterReceipt(dec("24"), dec("0.30"), dec("24"), dec("0.40")))
                    .isEqualByComparingTo("0.35");
        }

        @Test
        void unevenQuantitiesWeighProportionally() {
            // 10 kg a 8,00 + 30 kg a 6,00 -> (80 + 180) / 40 = 6,50
            assertThat(MovingAverage.afterReceipt(dec("10"), dec("8.00"), dec("30"), dec("6.00")))
                    .isEqualByComparingTo("6.50");
        }

        @Test
        void emptyStockTakesEntryCost() {
            assertThat(MovingAverage.afterReceipt(BigDecimal.ZERO, dec("5.00"), dec("12"), dec("1.25")))
                    .isEqualByComparingTo("1.25");
        }

        @Test
        void negativeStockTakesEntryCostInsteadOfAveragingWithIt() {
            // Con -5 ud a 2,00 y entrada de 10 a 3,00, promediar daría (−10 + 30) / 5 = 4,00: falso
            assertThat(MovingAverage.afterReceipt(dec("-5"), dec("2.00"), dec("10"), dec("3.00")))
                    .isEqualByComparingTo("3.00");
        }

        @Test
        void keepsSixDecimals() {
            // 3 a 1,00 + 4 a 2,00 -> 11 / 7 = 1,571428...
            BigDecimal average = MovingAverage.afterReceipt(dec("3"), dec("1.00"), dec("4"), dec("2.00"));

            assertThat(average).isEqualByComparingTo("1.571429");
            assertThat(average.scale()).isEqualTo(6);
        }

        @Test
        void entryAtCurrentAverageLeavesItUnchanged() {
            // Traspaso entre almacenes del mismo local: sale y vuelve a entrar al PMP vigente
            assertThat(MovingAverage.afterReceipt(dec("36"), dec("0.350000"), dec("12"), dec("0.350000")))
                    .isEqualByComparingTo("0.35");
        }

        @Test
        void entryNeedsPositiveQuantity() {
            assertThatThrownBy(() -> MovingAverage.afterReceipt(dec("1"), dec("1"), BigDecimal.ZERO, dec("1")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class FefoAllocation {

        private final Instant monday = Instant.parse("2026-10-19T08:00:00Z");
        private final Instant tuesday = Instant.parse("2026-10-20T08:00:00Z");

        @Test
        void consumesEarliestExpiryFirst() {
            List<Lot> lots = List.of(
                    new Lot(1L, LocalDate.of(2026, 11, 10), monday, dec("6")),
                    new Lot(2L, LocalDate.of(2026, 11, 2), tuesday, dec("6")));

            assertThat(LotAllocator.allocate(lots, dec("8"))).containsExactly(
                    new Allocation(2L, dec("6")),
                    new Allocation(1L, dec("2")));
        }

        @Test
        void lotsWithoutExpiryGoLast() {
            List<Lot> lots = List.of(
                    new Lot(1L, null, monday, dec("5")),
                    new Lot(2L, LocalDate.of(2026, 12, 1), tuesday, dec("5")));

            assertThat(LotAllocator.allocate(lots, dec("3"))).containsExactly(new Allocation(2L, dec("3")));
        }

        @Test
        void sameExpiryUsesOldestReceiptFirst() {
            LocalDate expiry = LocalDate.of(2026, 11, 5);
            List<Lot> lots = List.of(
                    new Lot(2L, expiry, tuesday, dec("4")),
                    new Lot(1L, expiry, monday, dec("4")));

            assertThat(LotAllocator.allocate(lots, dec("4"))).containsExactly(new Allocation(1L, dec("4")));
        }

        @Test
        void shortfallStaysUnallocated() {
            List<Lot> lots = List.of(new Lot(1L, LocalDate.of(2026, 11, 5), monday, dec("2")));

            assertThat(LotAllocator.allocate(lots, dec("5"))).containsExactly(new Allocation(1L, dec("2")));
        }

        @Test
        void emptyLotsAreSkipped() {
            List<Lot> lots = List.of(
                    new Lot(1L, LocalDate.of(2026, 11, 1), monday, BigDecimal.ZERO),
                    new Lot(2L, LocalDate.of(2026, 11, 9), tuesday, dec("3")));

            assertThat(LotAllocator.allocate(lots, dec("1"))).containsExactly(new Allocation(2L, dec("1")));
        }
    }

    @Nested
    class Replenishment {

        @Test
        void ordersUpToMaximumWhenBelowMinimum() {
            assertThat(ReplenishmentCalculator.suggestedStockQuantity(dec("48"), dec("96"), dec("46"), BigDecimal.ZERO))
                    .hasValueSatisfying(quantity -> assertThat(quantity).isEqualByComparingTo("50"));
        }

        @Test
        void pendingOrdersCountAsAvailable() {
            // 30 en almacén + 24 ya pedidas >= 48: no se vuelve a pedir
            assertThat(ReplenishmentCalculator.suggestedStockQuantity(dec("48"), dec("96"), dec("30"), dec("24")))
                    .isEmpty();
        }

        @Test
        void withoutMaximumOrdersUpToMinimum() {
            assertThat(ReplenishmentCalculator.suggestedStockQuantity(dec("10"), null, dec("4"), BigDecimal.ZERO))
                    .hasValueSatisfying(quantity -> assertThat(quantity).isEqualByComparingTo("6"));
        }

        @Test
        void withoutMinimumNeverSuggests() {
            assertThat(ReplenishmentCalculator.suggestedStockQuantity(null, dec("96"), BigDecimal.ZERO, BigDecimal.ZERO))
                    .isEmpty();
        }

        @Test
        void negativeStockIsCoveredToo() {
            assertThat(ReplenishmentCalculator.suggestedStockQuantity(dec("10"), dec("20"), dec("-3"), BigDecimal.ZERO))
                    .hasValueSatisfying(quantity -> assertThat(quantity).isEqualByComparingTo("23"));
        }

        @Test
        void roundsUpToWholePurchaseUnits() {
            assertThat(ReplenishmentCalculator.wholePurchaseUnits(dec("50"), dec("24"))).isEqualByComparingTo("3");
            assertThat(ReplenishmentCalculator.wholePurchaseUnits(dec("48"), dec("24"))).isEqualByComparingTo("2");
            assertThat(ReplenishmentCalculator.wholePurchaseUnits(dec("0.5"), dec("6"))).isEqualByComparingTo("1");
        }
    }

    private static BigDecimal dec(String value) {
        return new BigDecimal(value);
    }
}
