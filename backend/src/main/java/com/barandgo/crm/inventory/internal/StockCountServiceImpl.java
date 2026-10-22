package com.barandgo.crm.inventory.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.identity.UserRole;
import com.barandgo.crm.inventory.MovementSource;
import com.barandgo.crm.inventory.MovementType;
import com.barandgo.crm.inventory.StockCountDTOs.CountEntriesRequest;
import com.barandgo.crm.inventory.StockCountDTOs.CountEntry;
import com.barandgo.crm.inventory.StockCountDTOs.OpenCountRequest;
import com.barandgo.crm.inventory.StockCountDTOs.StockCountDTO;
import com.barandgo.crm.inventory.StockCountDTOs.StockCountStatus;
import com.barandgo.crm.inventory.StockCountService;
import com.barandgo.crm.inventory.StockCountDTOs;
import com.barandgo.crm.purchasing.StockItemDTO;
import com.barandgo.crm.shared.BadRequestException;
import com.barandgo.crm.shared.ConflictException;
import com.barandgo.crm.shared.ResourceNotFoundException;

/**
 * Recuentos. Supuesto de uso: se cuenta con el almacén parado (antes de abrir o tras cerrar). El
 * teórico se toma en el momento del cierre, con el saldo bloqueado, y el ajuste lleva el stock a lo
 * contado. Si se vende mientras se cuenta, esa venta se absorbe en la diferencia.
 */
@Service
@Transactional
public class StockCountServiceImpl implements StockCountService {

    private static final Logger logger = LoggerFactory.getLogger(StockCountServiceImpl.class);

    private final StockCountRepository countRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockBalanceRepository balanceRepository;
    private final StockValuationRepository valuationRepository;
    private final StockLedger ledger;
    private final ItemCatalog itemCatalog;

    public StockCountServiceImpl(StockCountRepository countRepository, WarehouseRepository warehouseRepository,
                                 StockBalanceRepository balanceRepository,
                                 StockValuationRepository valuationRepository, StockLedger ledger,
                                 ItemCatalog itemCatalog) {
        this.countRepository = countRepository;
        this.warehouseRepository = warehouseRepository;
        this.balanceRepository = balanceRepository;
        this.valuationRepository = valuationRepository;
        this.ledger = ledger;
        this.itemCatalog = itemCatalog;
    }

    @Override
    public StockCountDTO openCount(OpenCountRequest request, CurrentUser requester) {
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .filter(Warehouse::getActive)
                .orElseThrow(() -> new BadRequestException("Almacén inexistente o desactivado: " + request.warehouseId()));
        if (countRepository.existsByWarehouseIdAndStatusNot(warehouse.getId(), StockCountStatus.CLOSED)) {
            throw new ConflictException("Ya hay un recuento abierto en '" + warehouse.getName() + "'");
        }

        // Hoja de recuento: los artículos activos que han tenido stock en este almacén
        Map<Long, StockItemDTO> items = itemCatalog.all();
        StockCount count = new StockCount(warehouse.getId(), request.notes(), requester.id());
        balanceRepository.findByIdWarehouseId(warehouse.getId()).stream()
                .map(StockBalance::getStockItemId)
                .filter(itemId -> Boolean.TRUE.equals(items.get(itemId).active()))
                .forEach(count::lineFor);

        StockCount saved = countRepository.save(count);
        logger.info("Recuento {} abierto en '{}' con {} artículos", saved.getId(), warehouse.getName(),
                saved.getLines().size());
        return toDTO(saved, requester);
    }

    @Override
    public StockCountDTO recordCounts(Long countId, CountEntriesRequest request, CurrentUser requester) {
        StockCount count = findCount(countId);
        count.ensureNotClosed();
        for (CountEntry entry : request.entries()) {
            ItemCatalog.Item item = itemCatalog.get(entry.stockItemId());
            count.lineFor(item.id()).recordCount(item.toStockUnit(entry.countedQuantity(), entry.unit()),
                    requester.id());
        }
        count.markInProgress();
        return toDTO(countRepository.save(count), requester);
    }

    @Override
    public StockCountDTO closeCount(Long countId, CurrentUser requester) {
        StockCount count = findCount(countId);
        count.ensureNotClosed();
        List<StockCountLine> counted = count.getLines().stream().filter(StockCountLine::isCounted).toList();
        if (counted.isEmpty()) {
            throw new BadRequestException("No se puede cerrar un recuento sin ninguna cantidad contada");
        }
        Warehouse warehouse = warehouseRepository.findById(count.getWarehouseId()).orElseThrow();

        Map<Long, BigDecimal> theoretical = ledger.lockedQuantities(warehouse.getId(),
                counted.stream().map(StockCountLine::getStockItemId).toList());
        List<StockLedger.Entry> adjustments = new ArrayList<>();
        for (StockCountLine line : counted) {
            BigDecimal averageCost = valuationRepository
                    .findById(new StockValuation.Key(line.getStockItemId(), warehouse.getSiteId()))
                    .map(StockValuation::getAverageCost)
                    .orElse(BigDecimal.ZERO);
            line.settle(theoretical.get(line.getStockItemId()), averageCost);
            if (line.getDifference().signum() != 0) {
                adjustments.add(new StockLedger.Entry(MovementType.ADJUSTMENT, line.getStockItemId(),
                        warehouse.getId(), line.getDifference(), null, MovementSource.STOCK_COUNT, count.getId(),
                        line.getId(), null, "Recuento " + count.getId()));
            }
        }
        if (!adjustments.isEmpty()) {
            ledger.post(UUID.randomUUID(), adjustments, requester.id());
        }
        count.close(requester.id());

        logger.info("Recuento {} cerrado: {} ajustes en '{}'", count.getId(), adjustments.size(), warehouse.getName());
        return toDTO(countRepository.save(count), requester);
    }

    @Override
    @Transactional(readOnly = true)
    public StockCountDTO getCount(Long countId, CurrentUser requester) {
        return toDTO(findCount(countId), requester);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockCountDTO> getCounts(Long warehouseId, CurrentUser requester) {
        return countRepository.findAllByOrderByIdDesc().stream()
                .filter(count -> warehouseId == null || warehouseId.equals(count.getWarehouseId()))
                .map(count -> toDTO(count, requester))
                .toList();
    }

    private StockCount findCount(Long id) {
        return countRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recuento no encontrado con ID: " + id));
    }

    // Quien cuenta no ve el teórico ni los costes (contaría lo que espera encontrar)
    private StockCountDTO toDTO(StockCount count, CurrentUser requester) {
        boolean showResults = requester.role() != UserRole.STAFF;
        Map<Long, StockItemDTO> items = itemCatalog.all();
        String warehouseName = warehouseRepository.findById(count.getWarehouseId()).map(Warehouse::getName).orElse(null);

        BigDecimal totalDifference = BigDecimal.ZERO;
        List<StockCountDTOs.Line> lines = new ArrayList<>();
        for (StockCountLine line : count.getLines().stream()
                .sorted(Comparator.comparing(l -> items.get(l.getStockItemId()).name())).toList()) {
            StockItemDTO item = items.get(line.getStockItemId());
            BigDecimal differenceValue = line.getDifference() == null ? null
                    : line.getDifference().multiply(line.getUnitCost()).setScale(2, RoundingMode.HALF_UP);
            if (differenceValue != null) {
                totalDifference = totalDifference.add(differenceValue);
            }
            lines.add(new StockCountDTOs.Line(line.getId(), item.id(), item.code(), item.name(), item.stockUnit(),
                    line.getCountedQuantity(),
                    showResults ? line.getTheoreticalQuantity() : null,
                    showResults ? line.getDifference() : null,
                    showResults ? line.getUnitCost() : null,
                    showResults ? differenceValue : null));
        }
        return new StockCountDTO(count.getId(), count.getWarehouseId(), warehouseName, count.getStatus(),
                count.getNotes(), count.getOpenedBy(), count.getOpenedAt(), count.getClosedBy(), count.getClosedAt(),
                lines, showResults ? totalDifference : null);
    }
}
