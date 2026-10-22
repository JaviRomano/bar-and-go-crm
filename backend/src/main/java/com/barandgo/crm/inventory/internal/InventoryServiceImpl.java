package com.barandgo.crm.inventory.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.inventory.InventoryDTOs.AdjustmentRequestDTO;
import com.barandgo.crm.inventory.InventoryDTOs.ExpiringLotDTO;
import com.barandgo.crm.inventory.InventoryDTOs.StockBalanceDTO;
import com.barandgo.crm.inventory.InventoryDTOs.StockMovementDTO;
import com.barandgo.crm.inventory.InventoryDTOs.TransferRequestDTO;
import com.barandgo.crm.inventory.InventoryDTOs.ValuationLineDTO;
import com.barandgo.crm.inventory.InventoryDTOs.ValuationReportDTO;
import com.barandgo.crm.inventory.InventoryService;
import com.barandgo.crm.inventory.MovementSource;
import com.barandgo.crm.inventory.MovementType;
import com.barandgo.crm.inventory.StockPostingService;
import com.barandgo.crm.purchasing.StockItemDTO;
import com.barandgo.crm.shared.BadRequestException;
import com.barandgo.crm.shared.ResourceNotFoundException;

import jakarta.persistence.criteria.Predicate;

@Service
@Transactional
public class InventoryServiceImpl implements InventoryService, StockPostingService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceImpl.class);

    // Lo que pueden registrar otros módulos; recepción, ajustes y traspasos tienen su propio flujo
    private static final Set<MovementType> EXTERNAL_TYPES = EnumSet.of(MovementType.PRODUCTION_CONSUMPTION,
            MovementType.PRODUCTION_OUTPUT, MovementType.SALE, MovementType.WASTE);

    private final StockLedger ledger;
    private final ItemCatalog itemCatalog;
    private final WarehouseRepository warehouseRepository;
    private final StockBalanceRepository balanceRepository;
    private final StockValuationRepository valuationRepository;
    private final StockMovementRepository movementRepository;
    private final StockLotRepository lotRepository;

    public InventoryServiceImpl(StockLedger ledger, ItemCatalog itemCatalog, WarehouseRepository warehouseRepository,
                                StockBalanceRepository balanceRepository,
                                StockValuationRepository valuationRepository,
                                StockMovementRepository movementRepository, StockLotRepository lotRepository) {
        this.ledger = ledger;
        this.itemCatalog = itemCatalog;
        this.warehouseRepository = warehouseRepository;
        this.balanceRepository = balanceRepository;
        this.valuationRepository = valuationRepository;
        this.movementRepository = movementRepository;
        this.lotRepository = lotRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockBalanceDTO> getStock(Long warehouseId, Long stockItemId) {
        Map<Long, StockItemDTO> items = itemCatalog.all();
        Map<Long, Warehouse> warehouses = warehousesById();

        return balanceRepository.findAll().stream()
                .filter(b -> warehouseId == null || warehouseId.equals(b.getWarehouseId()))
                .filter(b -> stockItemId == null || stockItemId.equals(b.getStockItemId()))
                .sorted(Comparator.comparing((StockBalance b) -> items.get(b.getStockItemId()).name())
                        .thenComparing(b -> warehouses.get(b.getWarehouseId()).getName()))
                .map(b -> {
                    StockItemDTO item = items.get(b.getStockItemId());
                    return new StockBalanceDTO(item.id(), item.code(), item.name(), item.stockUnit(),
                            b.getWarehouseId(), warehouses.get(b.getWarehouseId()).getName(), b.getQuantity());
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ValuationReportDTO getValuation() {
        Map<Long, StockItemDTO> items = itemCatalog.all();
        List<ValuationLineDTO> lines = valuationRepository.findAll().stream()
                .filter(v -> v.getQuantityOnHand().signum() != 0)
                .map(v -> {
                    StockItemDTO item = items.get(v.getStockItemId());
                    BigDecimal value = v.getQuantityOnHand().multiply(v.getAverageCost())
                            .setScale(2, RoundingMode.HALF_UP);
                    return new ValuationLineDTO(item.id(), item.code(), item.name(), item.stockUnit(),
                            v.getSiteId(), v.getQuantityOnHand(), v.getAverageCost(), value);
                })
                .sorted(Comparator.comparing(ValuationLineDTO::stockItemName))
                .toList();
        BigDecimal total = lines.stream().map(ValuationLineDTO::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ValuationReportDTO(lines, total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockMovementDTO> getMovements(Long stockItemId, Long warehouseId, Instant from, Instant to) {
        Specification<StockMovement> filter = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (stockItemId != null) {
                predicates.add(cb.equal(root.get("stockItemId"), stockItemId));
            }
            if (warehouseId != null) {
                predicates.add(cb.equal(root.get("warehouseId"), warehouseId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("occurredAt"), to));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        List<StockMovement> movements = movementRepository.findAll(filter,
                Sort.by(Sort.Direction.DESC, "occurredAt", "id"));
        return toDTOs(movements);
    }

    @Override
    public List<StockMovementDTO> adjust(AdjustmentRequestDTO request, CurrentUser requester) {
        activeWarehouse(request.warehouseId());
        ItemCatalog.Item item = itemCatalog.get(request.stockItemId());
        BigDecimal quantity = item.toStockUnit(request.quantity(), request.unit());
        if (quantity.signum() == 0) {
            throw new BadRequestException("La cantidad del ajuste no puede ser cero");
        }
        BigDecimal unitCost = quantity.signum() > 0 ? request.unitCost() : null;

        List<StockLedger.Posted> posted = ledger.post(UUID.randomUUID(), List.of(new StockLedger.Entry(
                MovementType.ADJUSTMENT, item.id(), request.warehouseId(), quantity, unitCost,
                MovementSource.MANUAL, null, null, null, request.reason())), requester.id());

        logger.info("Ajuste manual de {} {} del artículo {} en el almacén {}", quantity, item.stockUnit(),
                item.id(), request.warehouseId());
        return toDTOs(posted.stream().map(StockLedger.Posted::movement).toList());
    }

    /*
     * La salida consume lotes FEFO en origen; la entrada recrea esos mismos lotes (código y caducidad) en
     * destino. Lo que no estaba en ningún lote entra sin lote. El PMP no cambia: es por local.
     */
    @Override
    public List<StockMovementDTO> transfer(TransferRequestDTO request, CurrentUser requester) {
        if (request.fromWarehouseId().equals(request.toWarehouseId())) {
            throw new BadRequestException("El almacén de origen y el de destino deben ser distintos");
        }
        Warehouse from = activeWarehouse(request.fromWarehouseId());
        Warehouse to = activeWarehouse(request.toWarehouseId());
        if (!from.getSiteId().equals(to.getSiteId())) {
            throw new BadRequestException("Solo se traspasa entre almacenes del mismo local");
        }
        ItemCatalog.Item item = itemCatalog.get(request.stockItemId());
        BigDecimal quantity = item.toStockUnit(request.quantity(), request.unit());

        UUID operationId = UUID.randomUUID();
        StockLedger.Posted out = ledger.post(operationId, List.of(new StockLedger.Entry(MovementType.TRANSFER,
                item.id(), from.getId(), quantity.negate(), null, MovementSource.TRANSFER, null, null, null,
                request.notes())), requester.id()).get(0);

        List<StockLedger.Entry> inbound = new ArrayList<>();
        BigDecimal withLot = BigDecimal.ZERO;
        for (LotAllocator.Allocation allocation : out.lotAllocations()) {
            StockLot source = lotRepository.findById(allocation.lotId()).orElseThrow();
            inbound.add(new StockLedger.Entry(MovementType.TRANSFER, item.id(), to.getId(), allocation.quantity(),
                    null, MovementSource.TRANSFER, null, null,
                    new StockLedger.NewLot(source.getLotCode(), source.getExpiryDate()), request.notes()));
            withLot = withLot.add(allocation.quantity());
        }
        BigDecimal withoutLot = quantity.subtract(withLot);
        if (withoutLot.signum() > 0) {
            inbound.add(new StockLedger.Entry(MovementType.TRANSFER, item.id(), to.getId(), withoutLot, null,
                    MovementSource.TRANSFER, null, null, null, request.notes()));
        }
        List<StockLedger.Posted> in = ledger.post(operationId, inbound, requester.id());

        logger.info("Traspaso de {} {} del artículo {}: {} -> {}", quantity, item.stockUnit(), item.id(),
                from.getName(), to.getName());
        List<StockMovement> movements = new ArrayList<>();
        movements.add(out.movement());
        in.forEach(p -> movements.add(p.movement()));
        return toDTOs(movements);
    }

    @Override
    public PostingResult post(UUID operationId, MovementType type, MovementSource source, Long sourceId,
                              Long warehouseId, List<PostingLine> lines, String notes, CurrentUser user) {
        if (!EXTERNAL_TYPES.contains(type)) {
            throw new BadRequestException("Tipo de movimiento no admitido desde otros módulos: " + type);
        }
        if (lines.isEmpty()) {
            throw new BadRequestException("No hay movimientos que registrar");
        }
        activeWarehouse(warehouseId);
        UUID operation = operationId != null ? operationId : UUID.randomUUID();
        List<StockLedger.Entry> entries = lines.stream()
                .map(line -> new StockLedger.Entry(type, line.stockItemId(), warehouseId, line.quantity(),
                        line.unitCost(), source, sourceId, null, null, notes))
                .toList();
        List<StockMovement> movements = ledger.post(operation, entries, user.id()).stream()
                .map(StockLedger.Posted::movement)
                .toList();
        BigDecimal total = movements.stream().map(StockMovement::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PostingResult(operation, toDTOs(movements), total);
    }

    @Override
    public PostingResult reverse(MovementSource source, Long sourceId, String notes, CurrentUser user) {
        List<StockMovement> movements = movementRepository.findBySourceTypeAndSourceIdOrderById(source, sourceId);
        Set<Long> alreadyReversed = movements.stream()
                .map(StockMovement::getReversesMovementId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        List<StockLedger.Entry> entries = movements.stream()
                .filter(m -> m.getReversesMovementId() == null && !alreadyReversed.contains(m.getId()))
                .map(m -> new StockLedger.Entry(m.getType(), m.getStockItemId(), m.getWarehouseId(),
                        m.getQuantity().negate(), m.getUnitCost(), source, sourceId, m.getSourceLineId(), null, notes,
                        m.getId()))
                .toList();
        UUID operation = UUID.randomUUID();
        if (entries.isEmpty()) {
            return new PostingResult(operation, List.of(), BigDecimal.ZERO);
        }
        List<StockMovement> reversals = ledger.post(operation, entries, user.id()).stream()
                .map(StockLedger.Posted::movement)
                .toList();
        BigDecimal total = reversals.stream().map(StockMovement::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        logger.info("Anulados {} movimientos de {} {}", reversals.size(), source, sourceId);
        return new PostingResult(operation, toDTOs(reversals), total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpiringLotDTO> getExpiringLots(int days) {
        if (days < 0) {
            throw new BadRequestException("El número de días no puede ser negativo");
        }
        LocalDate today = LocalDate.now();
        Map<Long, StockItemDTO> items = itemCatalog.all();
        Map<Long, Warehouse> warehouses = warehousesById();

        return lotRepository.findExpiringUntil(today.plusDays(days)).stream()
                .map(lot -> {
                    StockItemDTO item = items.get(lot.getStockItemId());
                    return new ExpiringLotDTO(lot.getId(), item.id(), item.code(), item.name(), lot.getWarehouseId(),
                            warehouses.get(lot.getWarehouseId()).getName(), lot.getLotCode(), lot.getExpiryDate(),
                            lot.getRemainingQuantity(), item.stockUnit(),
                            ChronoUnit.DAYS.between(today, lot.getExpiryDate()));
                })
                .toList();
    }

    private Warehouse activeWarehouse(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Almacén no encontrado con ID: " + id));
        if (!Boolean.TRUE.equals(warehouse.getActive())) {
            throw new BadRequestException("El almacén '" + warehouse.getName() + "' está desactivado");
        }
        return warehouse;
    }

    private Map<Long, Warehouse> warehousesById() {
        return warehouseRepository.findAll().stream().collect(Collectors.toMap(Warehouse::getId, Function.identity()));
    }

    private List<StockMovementDTO> toDTOs(List<StockMovement> movements) {
        if (movements.isEmpty()) {
            return List.of();
        }
        Map<Long, StockItemDTO> items = itemCatalog.all();
        Map<Long, Warehouse> warehouses = warehousesById();
        Map<Long, String> lotCodes = lotRepository.findAllById(movements.stream()
                        .map(StockMovement::getLotId).filter(id -> id != null).distinct().toList())
                .stream().collect(Collectors.toMap(StockLot::getId, StockLot::getLotCode));

        return movements.stream().map(m -> {
            StockItemDTO item = items.get(m.getStockItemId());
            return new StockMovementDTO(m.getId(), m.getOperationId(), m.getType(), item.id(), item.code(),
                    item.name(), m.getWarehouseId(), warehouses.get(m.getWarehouseId()).getName(),
                    lotCodes.get(m.getLotId()), m.getQuantity(), item.stockUnit(), m.getUnitCost(), m.getTotalCost(),
                    m.getSourceType(), m.getSourceId(), m.getUserId(), m.getNotes(), m.getOccurredAt());
        }).toList();
    }
}
