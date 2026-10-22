package com.barandgo.crm.inventory.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.barandgo.crm.inventory.MovementSource;
import com.barandgo.crm.inventory.MovementType;
import com.barandgo.crm.shared.BadRequestException;
import com.barandgo.crm.shared.ResourceNotFoundException;

/**
 * Única vía para cambiar el stock. Cada entrada genera un movimiento inmutable y, en la misma
 * transacción, actualiza el saldo del almacén, la valoración PMP del local y los lotes.
 *
 * <p>Concurrencia: antes de aplicar nada se bloquean (SELECT ... FOR UPDATE) todas las filas de saldo
 * y valoración que va a tocar la operación, siempre en el mismo orden (artículo, almacén / local), para
 * que dos operaciones simultáneas no se bloqueen mutuamente.
 */
@Component
class StockLedger {

    /** Lote nuevo que crea una entrada (artículos perecederos). */
    record NewLot(String lotCode, LocalDate expiryDate) {
    }

    /**
     * Una línea de la operación.
     *
     * @param quantity con signo, en unidad de inventario
     * @param unitCost coste de una entrada; null para valorar al PMP vigente. En salidas se ignora
     */
    record Entry(MovementType type, Long stockItemId, Long warehouseId, BigDecimal quantity, BigDecimal unitCost,
                 MovementSource sourceType, Long sourceId, Long sourceLineId, NewLot newLot, String notes,
                 Long reversesMovementId) {

        Entry(MovementType type, Long stockItemId, Long warehouseId, BigDecimal quantity, BigDecimal unitCost,
              MovementSource sourceType, Long sourceId, Long sourceLineId, NewLot newLot, String notes) {
            this(type, stockItemId, warehouseId, quantity, unitCost, sourceType, sourceId, sourceLineId, newLot, notes,
                    null);
        }
    }

    record Posted(StockMovement movement, StockLot createdLot, List<LotAllocator.Allocation> lotAllocations) {
    }

    private final StockMovementRepository movementRepository;
    private final StockBalanceRepository balanceRepository;
    private final StockValuationRepository valuationRepository;
    private final StockLotRepository lotRepository;
    private final WarehouseRepository warehouseRepository;

    StockLedger(StockMovementRepository movementRepository, StockBalanceRepository balanceRepository,
                StockValuationRepository valuationRepository, StockLotRepository lotRepository,
                WarehouseRepository warehouseRepository) {
        this.movementRepository = movementRepository;
        this.balanceRepository = balanceRepository;
        this.valuationRepository = valuationRepository;
        this.lotRepository = lotRepository;
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    List<Posted> post(UUID operationId, List<Entry> entries, Long userId) {
        Map<Long, Warehouse> warehouses = warehouses(entries.stream().map(Entry::warehouseId).toList());
        Map<StockBalance.Key, StockBalance> balances = lockBalances(entries.stream()
                .map(e -> new StockBalance.Key(e.stockItemId(), e.warehouseId())).toList());
        Map<StockValuation.Key, StockValuation> valuations = lockValuations(entries.stream()
                .map(e -> new StockValuation.Key(e.stockItemId(), warehouses.get(e.warehouseId()).getSiteId()))
                .toList());

        Instant now = Instant.now();
        List<Posted> posted = new ArrayList<>();
        for (Entry entry : entries) {
            Warehouse warehouse = warehouses.get(entry.warehouseId());
            StockBalance balance = balances.get(new StockBalance.Key(entry.stockItemId(), entry.warehouseId()));
            StockValuation valuation = valuations.get(new StockValuation.Key(entry.stockItemId(), warehouse.getSiteId()));
            posted.add(apply(operationId, entry, warehouse, balance, valuation, userId, now));
        }
        return posted;
    }

    /** Saldos actuales bloqueados para el resto de la transacción (un recuento los compara con lo contado). */
    @Transactional(propagation = Propagation.MANDATORY)
    Map<Long, BigDecimal> lockedQuantities(Long warehouseId, List<Long> stockItemIds) {
        return lockBalances(stockItemIds.stream().map(id -> new StockBalance.Key(id, warehouseId)).toList())
                .values().stream()
                .collect(Collectors.toMap(StockBalance::getStockItemId, StockBalance::getQuantity));
    }

    private Posted apply(UUID operationId, Entry entry, Warehouse warehouse, StockBalance balance,
                         StockValuation valuation, Long userId, Instant now) {
        BigDecimal quantity = entry.quantity();
        if (quantity.signum() == 0) {
            throw new BadRequestException("Un movimiento de stock necesita una cantidad distinta de cero");
        }

        BigDecimal unitCost;
        StockLot createdLot = null;
        List<LotAllocator.Allocation> allocations = List.of();

        if (quantity.signum() > 0) {
            unitCost = entry.unitCost() != null ? entry.unitCost() : valuation.getAverageCost();
            valuation.receive(quantity, unitCost);
            if (entry.newLot() != null) {
                createdLot = lotRepository.save(new StockLot(entry.stockItemId(), warehouse.getId(),
                        entry.newLot().lotCode(), entry.newLot().expiryDate(), quantity, now));
            }
        } else {
            BigDecimal outgoing = quantity.negate();
            if (!entry.type().allowsNegativeStock() && balance.getQuantity().compareTo(outgoing) < 0) {
                throw new BadRequestException("Stock insuficiente del artículo " + entry.stockItemId() + " en '"
                        + warehouse.getName() + "': hay " + balance.getQuantity().stripTrailingZeros().toPlainString()
                        + " y se piden " + outgoing.stripTrailingZeros().toPlainString());
            }
            unitCost = valuation.getAverageCost();
            valuation.issue(outgoing);
            allocations = consumeLots(entry.stockItemId(), warehouse.getId(), outgoing);
        }
        balance.add(quantity);

        StockMovement movement = movementRepository.save(new StockMovement(operationId, entry.type(),
                entry.stockItemId(), warehouse.getId(), createdLot != null ? createdLot.getId() : null, quantity,
                unitCost.setScale(MovingAverage.COST_SCALE, RoundingMode.HALF_UP),
                quantity.multiply(unitCost).setScale(4, RoundingMode.HALF_UP),
                entry.sourceType(), entry.sourceId(), entry.sourceLineId(), entry.reversesMovementId(), userId,
                entry.notes(), now));
        return new Posted(movement, createdLot, allocations);
    }

    private List<LotAllocator.Allocation> consumeLots(Long stockItemId, Long warehouseId, BigDecimal quantity) {
        Map<Long, StockLot> lots = lotRepository.findOpenLots(stockItemId, warehouseId).stream()
                .collect(Collectors.toMap(StockLot::getId, Function.identity()));
        List<LotAllocator.Allocation> allocations = LotAllocator.allocate(
                lots.values().stream().map(StockLot::toAllocatable).toList(), quantity);
        allocations.forEach(a -> lots.get(a.lotId()).consume(a.quantity()));
        return allocations;
    }

    private Map<Long, Warehouse> warehouses(List<Long> ids) {
        Map<Long, Warehouse> found = warehouseRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Warehouse::getId, Function.identity()));
        ids.stream().filter(id -> !found.containsKey(id)).findFirst().ifPresent(id -> {
            throw new ResourceNotFoundException("Almacén no encontrado con ID: " + id);
        });
        return found;
    }

    private Map<StockBalance.Key, StockBalance> lockBalances(List<StockBalance.Key> keys) {
        Map<StockBalance.Key, StockBalance> locked = new HashMap<>();
        keys.stream().distinct()
                .sorted(Comparator.comparing(StockBalance.Key::getStockItemId)
                        .thenComparing(StockBalance.Key::getWarehouseId))
                .forEach(key -> {
                    balanceRepository.ensureExists(key.getStockItemId(), key.getWarehouseId());
                    locked.put(key, balanceRepository.lockByKey(key).orElseThrow());
                });
        return locked;
    }

    private Map<StockValuation.Key, StockValuation> lockValuations(List<StockValuation.Key> keys) {
        Map<StockValuation.Key, StockValuation> locked = new HashMap<>();
        keys.stream().distinct()
                .sorted(Comparator.comparing(StockValuation.Key::getStockItemId)
                        .thenComparing(StockValuation.Key::getSiteId))
                .forEach(key -> {
                    valuationRepository.ensureExists(key.getStockItemId(), key.getSiteId());
                    locked.put(key, valuationRepository.lockByKey(key).orElseThrow());
                });
        return locked;
    }
}
