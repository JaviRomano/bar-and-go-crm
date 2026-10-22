package com.barandgo.crm.costcontrol.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barandgo.crm.catalog.ProductDTO;
import com.barandgo.crm.catalog.ProductService;
import com.barandgo.crm.costcontrol.CostControlDTOs.ConsumptionLine;
import com.barandgo.crm.costcontrol.CostControlDTOs.ConsumptionReportDTO;
import com.barandgo.crm.costcontrol.CostControlDTOs.OrderStockPostingDTO;
import com.barandgo.crm.costcontrol.CostControlDTOs.WasteDTO;
import com.barandgo.crm.costcontrol.CostControlDTOs.WasteRequest;
import com.barandgo.crm.costcontrol.CostControlService;
import com.barandgo.crm.costcontrol.WasteReason;
import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.identity.UserRole;
import com.barandgo.crm.inventory.InventoryDTOs.StockMovementDTO;
import com.barandgo.crm.inventory.InventoryDTOs.ValuationLineDTO;
import com.barandgo.crm.inventory.InventoryService;
import com.barandgo.crm.inventory.MovementSource;
import com.barandgo.crm.inventory.MovementType;
import com.barandgo.crm.inventory.StockCountDTOs.StockCountDTO;
import com.barandgo.crm.inventory.StockCountDTOs.StockCountStatus;
import com.barandgo.crm.inventory.StockCountService;
import com.barandgo.crm.inventory.StockPostingService;
import com.barandgo.crm.inventory.StockPostingService.PostingLine;
import com.barandgo.crm.inventory.StockPostingService.PostingResult;
import com.barandgo.crm.inventory.WarehouseDTO;
import com.barandgo.crm.inventory.WarehouseService;
import com.barandgo.crm.masterdata.UnitOfMeasureDTO;
import com.barandgo.crm.masterdata.UnitOfMeasureService;
import com.barandgo.crm.ordering.OrderEvents.OrderDelivered;
import com.barandgo.crm.ordering.OrderEvents.OrderDeliveryReverted;
import com.barandgo.crm.purchasing.StockItemDTO;
import com.barandgo.crm.purchasing.StockItemService;
import com.barandgo.crm.purchasing.StockItemUnits;
import com.barandgo.crm.recipes.RecipeService;
import com.barandgo.crm.shared.BadRequestException;

import jakarta.persistence.criteria.Predicate;

@Service
@Transactional
public class CostControlServiceImpl implements CostControlService {

    private static final Logger logger = LoggerFactory.getLogger(CostControlServiceImpl.class);

    private static final int QUANTITY_SCALE = 4;
    private static final int MONEY_SCALE = 2;

    private final WasteRepository wasteRepository;
    private final OrderStockPostingRepository postingRepository;
    private final StockPostingService stockPosting;
    private final InventoryService inventoryService;
    private final StockCountService stockCountService;
    private final WarehouseService warehouseService;
    private final StockItemService stockItemService;
    private final UnitOfMeasureService unitService;
    private final ProductService productService;
    private final RecipeService recipeService;

    public CostControlServiceImpl(WasteRepository wasteRepository, OrderStockPostingRepository postingRepository,
                                  StockPostingService stockPosting, InventoryService inventoryService,
                                  StockCountService stockCountService, WarehouseService warehouseService,
                                  StockItemService stockItemService, UnitOfMeasureService unitService,
                                  ProductService productService, RecipeService recipeService) {
        this.wasteRepository = wasteRepository;
        this.postingRepository = postingRepository;
        this.stockPosting = stockPosting;
        this.inventoryService = inventoryService;
        this.stockCountService = stockCountService;
        this.warehouseService = warehouseService;
        this.stockItemService = stockItemService;
        this.unitService = unitService;
        this.productService = productService;
        this.recipeService = recipeService;
    }

    @Override
    public WasteDTO registerWaste(WasteRequest request, CurrentUser requester) {
        if ((request.stockItemId() == null) == (request.productId() == null)) {
            throw new BadRequestException("Indica un artículo o un producto de la carta, no ambos");
        }

        List<PostingLine> lines;
        Long unitId = null;
        if (request.stockItemId() != null) {
            StockItemDTO item = stockItemService.getStockItem(request.stockItemId());
            String unit = request.unit() == null || request.unit().isBlank() ? item.stockUnit() : request.unit();
            BigDecimal quantity = unit.equals(item.stockUnit())
                    ? request.quantity()
                    : StockItemUnits.converterFor(item, unitService.genericConverter())
                            .convert(request.quantity(), unit, item.stockUnit());
            unitId = unitService.getUnitByCode(unit).id();
            lines = toIssueLines(Map.of(item.id(), quantity));
        } else {
            // Un plato tirado o devuelto: sale lo que dice su escandallo, en raciones
            productService.getProductById(request.productId());
            Map<Long, BigDecimal> consumption = recipeService.consumptionForProduct(request.productId(),
                            request.quantity())
                    .orElseThrow(() -> new BadRequestException(
                            "El producto no tiene escandallo activo: registra la merma por artículo"));
            lines = toIssueLines(consumption);
        }
        if (lines.isEmpty()) {
            throw new BadRequestException("La merma no mueve ninguna cantidad de stock");
        }

        UUID operationId = UUID.randomUUID();
        PostingResult result = stockPosting.post(operationId, MovementType.WASTE, MovementSource.WASTE, null,
                request.warehouseId(), lines, wasteNotes(request), requester);
        Waste waste = wasteRepository.save(new Waste(request.stockItemId(), request.productId(), request.quantity(),
                unitId, request.reason(), request.warehouseId(), result.totalCost().negate(), request.notes(),
                requester.id(), operationId));
        logger.info("Merma {} ({}) registrada por {}: {} EUR", waste.getId(), waste.getReason(), requester.id(),
                waste.getValuedCost());
        return toDTOs(List.of(waste)).getFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WasteDTO> getWaste(Instant from, Instant to, WasteReason reason) {
        Specification<Waste> filter = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("occurredAt"), to));
            }
            if (reason != null) {
                predicates.add(cb.equal(root.get("reason"), reason));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return toDTOs(wasteRepository.findAll(filter, Sort.by(Sort.Direction.DESC, "occurredAt", "id")));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderStockPostingDTO> getOrderPosting(Long orderId) {
        return postingRepository.findById(orderId).map(posting -> new OrderStockPostingDTO(posting.getOrderId(),
                posting.getWarehouseId(), posting.getPostedAt(), posting.getTotalCost(),
                posting.getProductsWithoutRecipe(), posting.getReversedAt()));
    }

    /**
     * Descuenta del almacén de ventas lo que consumen los platos entregados según su escandallo. Si no hay
     * almacén de ventas falla a propósito: el evento queda pendiente en el registro y se reintenta.
     */
    void postDeliveredOrder(OrderDelivered event) {
        if (postingRepository.existsById(event.orderId())) {
            logger.info("Pedido {}: descuento de stock ya procesado o cancelado, se ignora", event.orderId());
            return;
        }
        WarehouseDTO warehouse = warehouseService.getSalesWarehouse()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay almacén de ventas: no se puede descontar el pedido " + event.orderId()));

        Map<Long, BigDecimal> consumption = new HashMap<>();
        int withoutRecipe = 0;
        for (OrderDelivered.Line line : event.lines()) {
            Optional<Map<Long, BigDecimal>> lineConsumption = line.productId() == null ? Optional.empty()
                    : recipeService.consumptionForProduct(line.productId(), BigDecimal.valueOf(line.quantity()));
            if (lineConsumption.isEmpty()) {
                withoutRecipe++;
                continue;
            }
            lineConsumption.get().forEach((itemId, quantity) -> consumption.merge(itemId, quantity, BigDecimal::add));
        }

        List<PostingLine> lines = toIssueLines(consumption);
        if (lines.isEmpty()) {
            postingRepository.save(OrderStockPosting.posted(event.orderId(), warehouse.id(), null, BigDecimal.ZERO,
                    withoutRecipe));
            logger.info("Pedido {}: sin escandallos, no mueve stock", event.orderId());
            return;
        }
        CurrentUser user = new CurrentUser(event.deliveredBy(), null, UserRole.STAFF);
        PostingResult result = stockPosting.post(null, MovementType.SALE, MovementSource.ORDER, event.orderId(),
                warehouse.id(), lines, "Pedido " + event.orderId(), user);
        postingRepository.save(OrderStockPosting.posted(event.orderId(), warehouse.id(), result.operationId(),
                result.totalCost().negate(), withoutRecipe));
        logger.info("Pedido {}: {} artículos descontados de '{}', coste {} ({} productos sin escandallo)",
                event.orderId(), lines.size(), warehouse.name(), result.totalCost().negate(), withoutRecipe);
    }

    /** Devuelve al stock lo que descontó un pedido entregado que se cancela después. */
    void revertDeliveredOrder(OrderDeliveryReverted event) {
        Optional<OrderStockPosting> existing = postingRepository.findById(event.orderId());
        if (existing.isEmpty()) {
            postingRepository.save(OrderStockPosting.cancelledBeforePosting(event.orderId()));
            logger.info("Pedido {}: cancelado antes de descontar stock", event.orderId());
            return;
        }
        OrderStockPosting posting = existing.get();
        if (posting.isReversed()) {
            return;
        }
        CurrentUser user = new CurrentUser(event.revertedBy(), null, UserRole.MANAGER);
        PostingResult result = stockPosting.reverse(MovementSource.ORDER, event.orderId(),
                "Cancelación del pedido " + event.orderId(), user);
        posting.markReversed(result.operationId());
        logger.info("Pedido {}: descuento de stock anulado ({} movimientos)", event.orderId(),
                result.movements().size());
    }

    @Override
    @Transactional(readOnly = true)
    public ConsumptionReportDTO consumptionReport(Long fromCountId, Long toCountId, CurrentUser requester) {
        StockCountDTO opening = stockCountService.getCount(fromCountId, requester);
        StockCountDTO closing = stockCountService.getCount(toCountId, requester);
        if (opening.status() != StockCountStatus.CLOSED || closing.status() != StockCountStatus.CLOSED) {
            throw new BadRequestException("Los dos recuentos deben estar cerrados");
        }
        if (!opening.warehouseId().equals(closing.warehouseId())) {
            throw new BadRequestException("Los dos recuentos deben ser del mismo almacén");
        }
        if (!opening.closedAt().isBefore(closing.closedAt())) {
            throw new BadRequestException("El recuento inicial debe cerrarse antes que el final");
        }
        Long warehouseId = opening.warehouseId();

        // Solo artículos contados en los dos recuentos: sin cantidad contada no hay consumo real
        Map<Long, BigDecimal> openingQty = countedQuantities(opening);
        Map<Long, BigDecimal> closingQty = countedQuantities(closing);

        Map<Long, ConsumptionCalculator.Flows> flows = new HashMap<>();
        for (StockMovementDTO movement : inventoryService.getMovements(null, warehouseId, opening.closedAt(),
                closing.closedAt())) {
            flows.computeIfAbsent(movement.stockItemId(), id -> new ConsumptionCalculator.Flows())
                    .add(movement.type(), movement.quantity());
        }

        Long siteId = warehouseService.getWarehouses(true).stream()
                .filter(w -> w.id().equals(warehouseId))
                .map(WarehouseDTO::siteId)
                .findFirst()
                .orElse(null);
        Map<Long, BigDecimal> costs = inventoryService.getValuation().lines().stream()
                .filter(line -> line.siteId().equals(siteId))
                .collect(Collectors.toMap(ValuationLineDTO::stockItemId, ValuationLineDTO::averageCost, (a, b) -> a));
        Map<Long, StockItemDTO> items = stockItemService.getStockItems(null, null).stream()
                .collect(Collectors.toMap(StockItemDTO::id, Function.identity()));

        List<ConsumptionLine> lines = new ArrayList<>();
        BigDecimal theoreticalValue = BigDecimal.ZERO;
        BigDecimal realValue = BigDecimal.ZERO;
        BigDecimal deviationValue = BigDecimal.ZERO;
        BigDecimal unexplainedValue = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> entry : openingQty.entrySet()) {
            Long itemId = entry.getKey();
            if (!closingQty.containsKey(itemId)) {
                continue;
            }
            ConsumptionCalculator.Flows itemFlows = flows.getOrDefault(itemId, new ConsumptionCalculator.Flows());
            ConsumptionCalculator.Result result = ConsumptionCalculator.calculate(entry.getValue(),
                    closingQty.get(itemId), itemFlows);
            BigDecimal unitCost = costs.getOrDefault(itemId, BigDecimal.ZERO);
            BigDecimal lineDeviationValue = money(result.deviation().multiply(unitCost));
            BigDecimal lineUnexplainedValue = money(result.unexplained().multiply(unitCost));
            StockItemDTO item = items.get(itemId);
            lines.add(new ConsumptionLine(itemId, item.code(), item.name(), item.stockUnit(), entry.getValue(),
                    itemFlows.inflows(), itemFlows.transfersOut(), closingQty.get(itemId), result.real(),
                    itemFlows.theoretical(), itemFlows.waste(), result.deviation(), result.deviationPct(),
                    result.unexplained(), unitCost, lineDeviationValue, lineUnexplainedValue));
            theoreticalValue = theoreticalValue.add(itemFlows.theoretical().multiply(unitCost));
            realValue = realValue.add(result.real().multiply(unitCost));
            deviationValue = deviationValue.add(lineDeviationValue);
            unexplainedValue = unexplainedValue.add(lineUnexplainedValue);
        }
        // Lo que más dinero se va sin explicar, primero
        lines.sort(Comparator.comparing((ConsumptionLine line) -> line.unexplainedValue().abs()).reversed()
                .thenComparing(ConsumptionLine::stockItemCode));
        return new ConsumptionReportDTO(warehouseId, fromCountId, toCountId, opening.closedAt(), closing.closedAt(),
                lines, money(theoreticalValue), money(realValue), money(deviationValue), money(unexplainedValue));
    }

    private static Map<Long, BigDecimal> countedQuantities(StockCountDTO count) {
        Map<Long, BigDecimal> counted = new TreeMap<>();
        count.lines().stream()
                .filter(line -> line.countedQuantity() != null)
                .forEach(line -> counted.put(line.stockItemId(), line.countedQuantity()));
        return counted;
    }

    /**
     * Salidas en unidad de inventario y escala de stock, ordenadas por artículo (los saldos se bloquean
     * siempre en el mismo orden). Descarta lo que se redondea a cero.
     */
    private static List<PostingLine> toIssueLines(Map<Long, BigDecimal> consumption) {
        return new TreeMap<>(consumption).entrySet().stream()
                .map(entry -> new PostingLine(entry.getKey(),
                        entry.getValue().setScale(QUANTITY_SCALE, RoundingMode.HALF_UP).negate(), null))
                .filter(line -> line.quantity().signum() < 0)
                .toList();
    }

    private static String wasteNotes(WasteRequest request) {
        String notes = "Merma: " + request.reason();
        return request.notes() == null || request.notes().isBlank() ? notes : notes + ". " + request.notes();
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private List<WasteDTO> toDTOs(List<Waste> wastes) {
        if (wastes.isEmpty()) {
            return List.of();
        }
        Map<Long, StockItemDTO> items = stockItemService.getStockItems(null, null).stream()
                .collect(Collectors.toMap(StockItemDTO::id, Function.identity()));
        Map<Long, String> products = productService.getAllProducts().stream()
                .collect(Collectors.toMap(ProductDTO::getId, ProductDTO::getName));
        Map<Long, String> units = unitService.getUnits().stream()
                .collect(Collectors.toMap(UnitOfMeasureDTO::id, UnitOfMeasureDTO::code));
        return wastes.stream().map(waste -> {
            StockItemDTO item = waste.getStockItemId() == null ? null : items.get(waste.getStockItemId());
            return new WasteDTO(waste.getId(), waste.getStockItemId(), item == null ? null : item.code(),
                    item == null ? null : item.name(), waste.getProductId(),
                    waste.getProductId() == null ? null : products.get(waste.getProductId()), waste.getQuantity(),
                    waste.getUnitId() == null ? "ración" : units.get(waste.getUnitId()), waste.getReason(),
                    waste.getWarehouseId(), waste.getValuedCost(), waste.getNotes(), waste.getRecordedBy(),
                    waste.getOccurredAt());
        }).toList();
    }
}
