package com.barandgo.crm.purchasing.internal;

import static com.barandgo.crm.purchasing.PurchaseOrderStatus.CANCELLED;
import static com.barandgo.crm.purchasing.PurchaseOrderStatus.DRAFT;
import static com.barandgo.crm.purchasing.PurchaseOrderStatus.PARTIALLY_RECEIVED;
import static com.barandgo.crm.purchasing.PurchaseOrderStatus.RECEIVED;
import static com.barandgo.crm.purchasing.PurchaseOrderStatus.SENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import com.barandgo.crm.purchasing.PurchaseOrderStatus;
import com.barandgo.crm.shared.BadRequestException;
import com.barandgo.crm.shared.ConflictException;

class PurchaseOrderLifecycleTest {

    private static final Map<PurchaseOrderStatus, Set<PurchaseOrderStatus>> ALLOWED = Map.of(
            DRAFT, EnumSet.of(SENT, CANCELLED),
            SENT, EnumSet.of(PARTIALLY_RECEIVED, RECEIVED, CANCELLED),
            PARTIALLY_RECEIVED, EnumSet.of(RECEIVED),
            RECEIVED, EnumSet.noneOf(PurchaseOrderStatus.class),
            CANCELLED, EnumSet.noneOf(PurchaseOrderStatus.class));

    @ParameterizedTest
    @EnumSource(PurchaseOrderStatus.class)
    void transitionsMatchTheLifecycle(PurchaseOrderStatus from) {
        for (PurchaseOrderStatus to : PurchaseOrderStatus.values()) {
            assertThat(from.canTransitionTo(to))
                    .as("%s -> %s", from, to)
                    .isEqualTo(ALLOWED.get(from).contains(to));
        }
    }

    @Test
    void draftWithLinesCanBeSent() {
        PurchaseOrder order = orderWithOneLine();

        order.send();

        assertThat(order.getStatus()).isEqualTo(SENT);
        assertThat(order.getSentAt()).isNotNull();
    }

    @Test
    void emptyDraftCannotBeSent() {
        assertThatThrownBy(() -> new PurchaseOrder().send())
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void sentOrderIsNoLongerEditable() {
        PurchaseOrder order = orderWithOneLine();
        order.send();

        assertThatThrownBy(order::ensureEditable).isInstanceOf(ConflictException.class);
    }

    @Test
    void cancelledOrderCannotBeCancelledAgain() {
        PurchaseOrder order = orderWithOneLine();
        order.cancel();

        assertThat(order.getCancelledAt()).isNotNull();
        assertThatThrownBy(order::cancel).isInstanceOf(ConflictException.class);
    }

    @Test
    void receivedOrderCannotBeCancelled() {
        PurchaseOrder order = orderWithOneLine();
        order.send();
        order.transitionTo(RECEIVED);

        assertThatThrownBy(order::cancel).isInstanceOf(ConflictException.class);
    }

    @Test
    void partialReceiptThenRestCompletesTheOrder() {
        PurchaseOrder order = orderWithLines("10", "5");
        order.send();
        PurchaseOrderLine first = order.getLines().get(0);
        PurchaseOrderLine second = order.getLines().get(1);

        order.registerReceipt(Map.of(first.getId(), new BigDecimal("10"), second.getId(), new BigDecimal("2")));
        assertThat(order.getStatus()).isEqualTo(PARTIALLY_RECEIVED);

        // Una segunda recepción parcial no cambia de estado (no es una transición)
        order.registerReceipt(Map.of(second.getId(), new BigDecimal("1")));
        assertThat(order.getStatus()).isEqualTo(PARTIALLY_RECEIVED);

        order.registerReceipt(Map.of(second.getId(), new BigDecimal("2")));
        assertThat(order.getStatus()).isEqualTo(RECEIVED);
        assertThat(second.getReceivedQuantity()).isEqualByComparingTo("5");
    }

    @Test
    void receivingMoreThanOrderedCompletesTheLine() {
        PurchaseOrder order = orderWithLines("4");
        order.send();

        order.registerReceipt(Map.of(order.getLines().get(0).getId(), new BigDecimal("5")));

        assertThat(order.getStatus()).isEqualTo(RECEIVED);
    }

    @Test
    void draftCannotReceiveGoods() {
        PurchaseOrder order = orderWithLines("4");

        assertThatThrownBy(() -> order.registerReceipt(Map.of(order.getLines().get(0).getId(), BigDecimal.ONE)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void lineOfAnotherOrderIsRejected() {
        PurchaseOrder order = orderWithLines("4");
        order.send();

        assertThatThrownBy(() -> order.registerReceipt(Map.of(999L, BigDecimal.ONE)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void partiallyReceivedOrderCanBeClosedShort() {
        PurchaseOrder order = orderWithLines("10");
        order.send();
        order.registerReceipt(Map.of(order.getLines().get(0).getId(), new BigDecimal("6")));

        order.close();

        assertThat(order.getStatus()).isEqualTo(RECEIVED);
    }

    @Test
    void draftCannotBeClosed() {
        assertThatThrownBy(() -> orderWithLines("1").close()).isInstanceOf(ConflictException.class);
    }

    private static PurchaseOrder orderWithLines(String... quantities) {
        PurchaseOrder order = new PurchaseOrder();
        long id = 1;
        for (String quantity : quantities) {
            PurchaseOrderLine line = new PurchaseOrderLine();
            ReflectionTestUtils.setField(line, "id", id++);
            line.setQuantity(new BigDecimal(quantity));
            line.setUnitPrice(BigDecimal.ONE);
            line.setVatRate(BigDecimal.TEN);
            order.addLine(line);
        }
        return order;
    }

    private static PurchaseOrder orderWithOneLine() {
        PurchaseOrderLine line = new PurchaseOrderLine();
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(BigDecimal.TEN);
        line.setVatRate(new BigDecimal("21"));
        PurchaseOrder order = new PurchaseOrder();
        order.addLine(line);
        return order;
    }
}
