package com.barandgo.crm.inventory.internal;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.inventory.ReplenishmentDTOs.CreateOrdersRequest;
import com.barandgo.crm.inventory.ReplenishmentDTOs.Line;
import com.barandgo.crm.inventory.ReplenishmentDTOs.SupplierGroup;
import com.barandgo.crm.inventory.ReplenishmentService;
import com.barandgo.crm.masterdata.SupplierDTO;
import com.barandgo.crm.masterdata.SupplierService;
import com.barandgo.crm.masterdata.UnitConverter;
import com.barandgo.crm.purchasing.PurchaseOrderDTO;
import com.barandgo.crm.purchasing.PurchaseOrderLineRequestDTO;
import com.barandgo.crm.purchasing.PurchaseOrderRequestDTO;
import com.barandgo.crm.purchasing.PurchaseOrderService;
import com.barandgo.crm.purchasing.PurchaseOrderStatus;
import com.barandgo.crm.purchasing.StockItemDTO;
import com.barandgo.crm.purchasing.StockItemService;
import com.barandgo.crm.purchasing.StockItemSupplierDTO;

/**
 * Sugerencia de reposición. Stock del local (todos sus almacenes) más lo pedido y pendiente de
 * recibir, comparado con el mínimo. Proveedor: el habitual si tiene tarifa; si no, el de tarifa más
 * barata por unidad de inventario; sin tarifa, el artículo queda en el grupo "sin proveedor".
 */
@Service
@Transactional
public class ReplenishmentServiceImpl implements ReplenishmentService {

    private static final Logger logger = LoggerFactory.getLogger(ReplenishmentServiceImpl.class);

    private final ItemCatalog itemCatalog;
    private final StockBalanceRepository balanceRepository;
    private final StockItemService stockItemService;
    private final PurchaseOrderService purchaseOrderService;
    private final SupplierService supplierService;

    public ReplenishmentServiceImpl(ItemCatalog itemCatalog, StockBalanceRepository balanceRepository,
                                    StockItemService stockItemService, PurchaseOrderService purchaseOrderService,
                                    SupplierService supplierService) {
        this.itemCatalog = itemCatalog;
        this.balanceRepository = balanceRepository;
        this.stockItemService = stockItemService;
        this.purchaseOrderService = purchaseOrderService;
        this.supplierService = supplierService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierGroup> getSuggestions() {
        UnitConverter generic = itemCatalog.genericConverter();
        Map<Long, StockItemDTO> items = itemCatalog.all();
        Map<Long, BigDecimal> onHand = balanceRepository.findAll().stream()
                .collect(Collectors.toMap(StockBalance::getStockItemId, StockBalance::getQuantity, BigDecimal::add));
        Map<Long, BigDecimal> onOrder = pendingOnOrder(items, generic);
        Map<Long, String> supplierNames = supplierService.getSuppliers(null).stream()
                .collect(Collectors.toMap(SupplierDTO::id, SupplierDTO::legalName));
        Set<Long> activeSuppliers = supplierService.getSuppliers(true).stream()
                .map(SupplierDTO::id)
                .collect(Collectors.toSet());

        Map<Long, List<Line>> bySupplier = new LinkedHashMap<>();
        List<Line> withoutSupplier = new ArrayList<>();
        items.values().stream()
                .filter(item -> Boolean.TRUE.equals(item.active()) && item.minStock() != null)
                .sorted(Comparator.comparing(StockItemDTO::name))
                .forEach(item -> {
                    BigDecimal hand = onHand.getOrDefault(item.id(), BigDecimal.ZERO);
                    BigDecimal ordered = onOrder.getOrDefault(item.id(), BigDecimal.ZERO);
                    ReplenishmentCalculator.suggestedStockQuantity(item.minStock(), item.maxStock(), hand, ordered)
                            .ifPresent(needed -> {
                                UnitConverter converter = ItemCatalog.converterFor(item, generic);
                                Optional<StockItemSupplierDTO> tariff = chooseTariff(item, converter, activeSuppliers);
                                String unit = tariff.map(StockItemSupplierDTO::purchaseUnit).orElse(item.purchaseUnit());
                                BigDecimal perUnit = converter.factor(unit, item.stockUnit());
                                Line line = new Line(item.id(), item.code(), item.name(), item.stockUnit(), hand,
                                        ordered, item.minStock(), item.maxStock(), needed, unit,
                                        ReplenishmentCalculator.wholePurchaseUnits(needed, perUnit),
                                        tariff.map(StockItemSupplierDTO::unitPrice).orElse(null));
                                tariff.ifPresentOrElse(
                                        t -> bySupplier.computeIfAbsent(t.supplierId(), id -> new ArrayList<>()).add(line),
                                        () -> withoutSupplier.add(line));
                            });
                });

        List<SupplierGroup> groups = new ArrayList<>();
        bySupplier.forEach((supplierId, lines) -> groups.add(new SupplierGroup(supplierId, supplierNames.get(supplierId),
                lines)));
        groups.sort(Comparator.comparing(SupplierGroup::supplierName));
        if (!withoutSupplier.isEmpty()) {
            groups.add(new SupplierGroup(null, null, withoutSupplier));
        }
        return groups;
    }

    @Override
    public List<PurchaseOrderDTO> createDraftOrders(CreateOrdersRequest request, CurrentUser requester) {
        List<Long> wanted = request == null || request.supplierIds() == null ? List.of() : request.supplierIds();
        List<PurchaseOrderDTO> created = new ArrayList<>();
        for (SupplierGroup group : getSuggestions()) {
            if (group.supplierId() == null || (!wanted.isEmpty() && !wanted.contains(group.supplierId()))) {
                continue;
            }
            List<PurchaseOrderLineRequestDTO> lines = group.lines().stream()
                    .map(line -> new PurchaseOrderLineRequestDTO(line.stockItemId(), line.suggestedPurchaseQuantity(),
                            line.purchaseUnit(), null))
                    .toList();
            created.add(purchaseOrderService.createPurchaseOrder(new PurchaseOrderRequestDTO(group.supplierId(), null,
                    "Generado desde la sugerencia de reposición", lines), requester));
        }
        logger.info("Reposición: {} pedidos en borrador creados", created.size());
        return created;
    }

    /** Pedido y no recibido, en unidad de inventario, de los pedidos enviados o recibidos en parte. */
    private Map<Long, BigDecimal> pendingOnOrder(Map<Long, StockItemDTO> items, UnitConverter generic) {
        Map<Long, BigDecimal> pending = new HashMap<>();
        Stream.of(PurchaseOrderStatus.SENT, PurchaseOrderStatus.PARTIALLY_RECEIVED)
                .flatMap(status -> purchaseOrderService.getPurchaseOrders(status, null).stream())
                .flatMap(order -> order.lines().stream())
                .forEach(line -> {
                    BigDecimal open = line.quantity().subtract(line.receivedQuantity());
                    StockItemDTO item = items.get(line.stockItemId());
                    if (open.signum() > 0 && item != null) {
                        BigDecimal inStockUnit = ItemCatalog.converterFor(item, generic)
                                .convert(open, line.unit(), item.stockUnit());
                        pending.merge(item.id(), inStockUnit.setScale(4, RoundingMode.HALF_UP), BigDecimal::add);
                    }
                });
        return pending;
    }

    private Optional<StockItemSupplierDTO> chooseTariff(StockItemDTO item, UnitConverter converter,
                                                        Set<Long> activeSuppliers) {
        List<StockItemSupplierDTO> tariffs = stockItemService.getSuppliers(item.id()).stream()
                .filter(t -> activeSuppliers.contains(t.supplierId()))
                .toList();
        Optional<StockItemSupplierDTO> preferred = tariffs.stream()
                .filter(t -> t.supplierId().equals(item.preferredSupplierId()))
                .findFirst();
        if (preferred.isPresent()) {
            return preferred;
        }
        // Precio por unidad de inventario para comparar tarifas en unidades distintas
        return tariffs.stream().min(Comparator.comparing((StockItemSupplierDTO t) -> t.unitPrice()
                .divide(converter.factor(t.purchaseUnit(), item.stockUnit()), MathContext.DECIMAL64)));
    }
}
