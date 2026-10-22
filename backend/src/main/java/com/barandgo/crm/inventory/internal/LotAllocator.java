package com.barandgo.crm.inventory.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Reparto de una salida entre lotes, FEFO: primero el que caduca antes; sin fecha de caducidad,
 * al final; a igual fecha, el que entró antes.
 *
 * <p>Es informativo (D12): la valoración sigue siendo PMP. Si los lotes no cubren la salida (stock
 * negativo o mercancía sin lote), lo que falta queda sin asignar.
 */
final class LotAllocator {

    record Lot(Long id, LocalDate expiryDate, Instant receivedAt, BigDecimal remaining) {
    }

    record Allocation(Long lotId, BigDecimal quantity) {
    }

    static final Comparator<Lot> FEFO = Comparator
            .comparing(Lot::expiryDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Lot::receivedAt)
            .thenComparing(Lot::id);

    private LotAllocator() {
    }

    static List<Allocation> allocate(List<Lot> lots, BigDecimal quantity) {
        List<Allocation> allocations = new ArrayList<>();
        BigDecimal pending = quantity;
        for (Lot lot : lots.stream().filter(l -> l.remaining().signum() > 0).sorted(FEFO).toList()) {
            if (pending.signum() <= 0) {
                break;
            }
            BigDecimal taken = lot.remaining().min(pending);
            allocations.add(new Allocation(lot.id(), taken));
            pending = pending.subtract(taken);
        }
        return allocations;
    }
}
