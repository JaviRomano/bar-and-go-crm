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
