package com.barandgo.crm.reporting.internal;

import static com.barandgo.crm.reporting.internal.ReportMath.money;
import static com.barandgo.crm.reporting.internal.ReportMath.net;
import static com.barandgo.crm.reporting.internal.ReportMath.pct;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barandgo.crm.catalog.ProductDTO;
import com.barandgo.crm.catalog.ProductService;
import com.barandgo.crm.costcontrol.CostControlDTOs.WasteDTO;
import com.barandgo.crm.costcontrol.CostControlService;
import com.barandgo.crm.costcontrol.WasteReason;
import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.inventory.InventoryDTOs.MovementTotalDTO;
import com.barandgo.crm.inventory.InventoryService;
import com.barandgo.crm.inventory.MovementType;
import com.barandgo.crm.inventory.WarehouseDTO;
import com.barandgo.crm.inventory.WarehouseService;
import com.barandgo.crm.masterdata.UnitConverter;
import com.barandgo.crm.masterdata.UnitOfMeasureService;
import com.barandgo.crm.ordering.OrderService;
import com.barandgo.crm.ordering.SoldLine;
import com.barandgo.crm.purchasing.PurchasePriceDTO;
import com.barandgo.crm.purchasing.StockItemDTO;
import com.barandgo.crm.purchasing.StockItemService;
import com.barandgo.crm.purchasing.StockItemUnits;
import com.barandgo.crm.recipes.RecipeDTOs.RecipeDTO;
import com.barandgo.crm.recipes.RecipeService;
import com.barandgo.crm.recipes.RecipeType;
import com.barandgo.crm.reporting.ReportDTOs.FoodCostPoint;
import com.barandgo.crm.reporting.ReportDTOs.FoodCostReport;
import com.barandgo.crm.reporting.ReportDTOs.GroupValue;
import com.barandgo.crm.reporting.ReportDTOs.MenuEngineeringReport;
import com.barandgo.crm.reporting.ReportDTOs.MenuItem;
import com.barandgo.crm.reporting.ReportDTOs.PricePoint;
import com.barandgo.crm.reporting.ReportDTOs.PriceSeries;
import com.barandgo.crm.reporting.ReportDTOs.RotationLine;
import com.barandgo.crm.reporting.ReportDTOs.RotationReport;
import com.barandgo.crm.reporting.ReportDTOs.StockValueLine;
import com.barandgo.crm.reporting.ReportDTOs.StockValueReport;
import com.barandgo.crm.reporting.ReportDTOs.SupplierPriceReport;
import com.barandgo.crm.reporting.ReportDTOs.WasteByItem;
import com.barandgo.crm.reporting.ReportDTOs.WasteByReason;
import com.barandgo.crm.reporting.ReportDTOs.WasteBySubject;
import com.barandgo.crm.reporting.ReportDTOs.WasteReport;
import com.barandgo.crm.reporting.ReportService;
import com.barandgo.crm.shared.BadRequestException;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    /** Hasta este número de días la serie del food cost va por días; por encima, por semanas. */
    private static final int DAILY_SERIES_MAX_DAYS = 31;

    private static final Set<MovementType> CONSUMPTION_TYPES =
            Set.of(MovementType.SALE, MovementType.WASTE, MovementType.PRODUCTION_CONSUMPTION);

    private final InventoryService inventoryService;
    private final WarehouseService warehouseService;
    private final StockItemService stockItemService;
    private final UnitOfMeasureService unitService;
    private final CostControlService costControlService;
    private final OrderService orderService;
    private final ProductService productService;
    private final RecipeService recipeService;

    public ReportServiceImpl(InventoryService inventoryService, WarehouseService warehouseService,
                             StockItemService stockItemService, UnitOfMeasureService unitService,
                             CostControlService costControlService, OrderService orderService,
                             ProductService productService, RecipeService recipeService) {
        this.inventoryService = inventoryService;
        this.warehouseService = warehouseService;
        this.stockItemService = stockItemService;
        this.unitService = unitService;
        this.costControlService = costControlService;
        this.orderService = orderService;
        this.productService = productService;
        this.recipeService = recipeService;
    }

    @Override
    public StockValueReport stockValue(Instant at, Long warehouseId) {
        Instant when = at == null ? Instant.now() : at;
        List<MovementTotalDTO> totals = inventoryService.getMovementTotals(warehouseId, null, when);
        Map<Long, StockItemDTO> items = items();
        Map<Long, String> warehouses = warehouseService.getWarehouses(true).stream()
                .collect(Collectors.toMap(WarehouseDTO::id, WarehouseDTO::name));

        Map<Long, BigDecimal[]> byItem = new HashMap<>();
        Map<Long, BigDecimal> byWarehouse = new TreeMap<>();
        for (MovementTotalDTO total : totals) {
            BigDecimal[] position = byItem.computeIfAbsent(total.stockItemId(),
                    id -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
            position[0] = position[0].add(total.quantity());
            position[1] = position[1].add(total.value());
            byWarehouse.merge(total.warehouseId(), total.value(), BigDecimal::add);
        }

        List<StockValueLine> lines = byItem.entrySet().stream()
                .filter(entry -> entry.getValue()[0].signum() != 0 || money(entry.getValue()[1]).signum() != 0)
                .map(entry -> {
                    StockItemDTO item = items.get(entry.getKey());
                    BigDecimal quantity = entry.getValue()[0];
                    BigDecimal value = money(entry.getValue()[1]);
                    BigDecimal averageCost = quantity.signum() > 0
                            ? entry.getValue()[1].divide(quantity, 4, RoundingMode.HALF_UP) : null;
                    return new StockValueLine(item.id(), item.code(), item.name(), item.itemCategory(),
                            item.stockUnit(), quantity, averageCost, value);
                })
                .sorted(Comparator.comparing(StockValueLine::value).reversed()
                        .thenComparing(StockValueLine::stockItemName))
                .toList();
        BigDecimal totalValue = lines.stream().map(StockValueLine::value).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> categoryValues = new TreeMap<>();
        Map<String, Long> categoryIds = new HashMap<>();
        for (StockValueLine line : lines) {
            String category = Objects.requireNonNullElse(line.category(), "Sin categoría");
            categoryValues.merge(category, line.value(), BigDecimal::add);
            categoryIds.putIfAbsent(category, items.get(line.stockItemId()).itemCategoryId());
        }
        List<GroupValue> byCategory = categoryValues.entrySet().stream()
                .map(entry -> new GroupValue(categoryIds.get(entry.getKey()), entry.getKey(), entry.getValue(),
                        pct(entry.getValue(), totalValue)))
                .sorted(Comparator.comparing(GroupValue::value).reversed())
                .toList();
        List<GroupValue> warehouseValues = byWarehouse.entrySet().stream()
                .map(entry -> new GroupValue(entry.getKey(), warehouses.get(entry.getKey()), money(entry.getValue()),
                        pct(money(entry.getValue()), totalValue)))
                .filter(group -> group.value().signum() != 0)
                .toList();
        return new StockValueReport(when, warehouseId, lines, byCategory, warehouseValues, totalValue);
    }

    @Override
    public FoodCostReport foodCost(Instant from, Instant to, Long warehouseId, CurrentUser requester) {
        checkPeriod(from, to);
        List<SoldLine> sales = orderService.getDeliveredSales(from, to);
        Map<Long, ProductDTO> products = products();
        Set<Long> withRecipe = activeDishes(requester).stream().map(RecipeDTO::productId).collect(Collectors.toSet());

        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal netRevenue = BigDecimal.ZERO;
        BigDecimal withoutRecipe = BigDecimal.ZERO;
        for (SoldLine line : sales) {
            BigDecimal lineGross = line.unitPrice().multiply(BigDecimal.valueOf(line.quantity()));
            BigDecimal lineNet = net(lineGross, vatOf(products.get(line.productId())));
            gross = gross.add(lineGross);
            netRevenue = netRevenue.add(lineNet);
            if (line.productId() == null || !withRecipe.contains(line.productId())) {
                withoutRecipe = withoutRecipe.add(lineNet);
            }
        }

        Map<MovementType, BigDecimal> byType = valueByType(inventoryService.getMovementTotals(warehouseId, from, to));
        BigDecimal theoretical = consumed(byType, MovementType.SALE);
        BigDecimal waste = consumed(byType, MovementType.WASTE);
        BigDecimal adjustments = consumed(byType, MovementType.ADJUSTMENT);
        BigDecimal production = consumed(byType, MovementType.PRODUCTION_CONSUMPTION)
                .add(consumed(byType, MovementType.PRODUCTION_OUTPUT));
        BigDecimal real = theoretical.add(waste).add(adjustments).add(production);

        BigDecimal theoreticalPct = pct(theoretical, netRevenue);
        BigDecimal realPct = pct(real, netRevenue);
        BigDecimal deviation = theoreticalPct == null || realPct == null ? null : realPct.subtract(theoreticalPct);

        boolean daily = Duration.between(from, to).toDays() <= DAILY_SERIES_MAX_DAYS;
        List<FoodCostPoint> series = series(from, to, daily ? 1 : 7, warehouseId, sales, products);
        return new FoodCostReport(from, to, warehouseId, money(gross), money(netRevenue), money(withoutRecipe),
                money(theoretical), money(waste), money(adjustments), money(production), money(real), theoreticalPct,
                realPct, deviation, daily ? "DAY" : "WEEK", series);
    }

    @Override
    public WasteReport waste(Instant from, Instant to, Long warehouseId) {
        checkPeriod(from, to);
        List<WasteDTO> wastes = costControlService.getWaste(from, to, null, warehouseId);
        BigDecimal total = wastes.stream().map(WasteDTO::valuedCost).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<WasteReason, List<WasteDTO>> grouped = wastes.stream()
                .collect(Collectors.groupingBy(WasteDTO::reason, () -> new EnumMap<>(WasteReason.class),
                        Collectors.toList()));
        List<WasteByReason> byReason = grouped.entrySet().stream()
                .map(entry -> {
                    BigDecimal value = money(entry.getValue().stream().map(WasteDTO::valuedCost)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                    return new WasteByReason(entry.getKey(), entry.getValue().size(), value, pct(value, total));
                })
                .sorted(Comparator.comparing(WasteByReason::value).reversed())
                .toList();

        Map<String, List<WasteDTO>> bySubjectKey = wastes.stream().collect(Collectors.groupingBy(
                w -> w.stockItemId() != null ? "ITEM:" + w.stockItemId() + ":" + w.unit() : "PRODUCT:" + w.productId(),
                LinkedHashMap::new, Collectors.toList()));
        List<WasteBySubject> bySubject = bySubjectKey.values().stream()
                .map(group -> {
                    WasteDTO first = group.getFirst();
                    boolean item = first.stockItemId() != null;
                    return new WasteBySubject(item ? "ITEM" : "PRODUCT", item ? first.stockItemId() : first.productId(),
                            item ? first.stockItemName() : first.productName(), group.size(),
                            group.stream().map(WasteDTO::quantity).reduce(BigDecimal.ZERO, BigDecimal::add),
                            first.unit(),
                            money(group.stream().map(WasteDTO::valuedCost).reduce(BigDecimal.ZERO, BigDecimal::add)));
                })
                .sorted(Comparator.comparing(WasteBySubject::value).reversed())
                .toList();

        Map<Long, StockItemDTO> items = items();
        Map<Long, BigDecimal[]> itemTotals = new HashMap<>();
        for (MovementTotalDTO movement : inventoryService.getMovementTotals(warehouseId, from, to)) {
            if (movement.type() == MovementType.WASTE) {
                BigDecimal[] sums = itemTotals.computeIfAbsent(movement.stockItemId(),
                        id -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
                sums[0] = sums[0].subtract(movement.quantity());
                sums[1] = sums[1].subtract(movement.value());
            }
        }
        List<WasteByItem> byItem = itemTotals.entrySet().stream()
                .map(entry -> {
                    StockItemDTO item = items.get(entry.getKey());
                    return new WasteByItem(item.id(), item.code(), item.name(), item.stockUnit(), entry.getValue()[0],
                            money(entry.getValue()[1]));
                })
                .sorted(Comparator.comparing(WasteByItem::value).reversed())
                .toList();
        return new WasteReport(from, to, warehouseId, money(total), byReason, bySubject, byItem);
    }

    @Override
    public RotationReport rotation(Instant from, Instant to, Long warehouseId) {
        checkPeriod(from, to);
        long days = Math.max(1, (long) Math.ceil(Duration.between(from, to).toHours() / 24.0));
        Map<Long, BigDecimal> opening = valueByItem(inventoryService.getMovementTotals(warehouseId, null, from));
        Map<Long, BigDecimal> closing = valueByItem(inventoryService.getMovementTotals(warehouseId, null, to));
        Map<Long, BigDecimal> consumption = new HashMap<>();
        for (MovementTotalDTO movement : inventoryService.getMovementTotals(warehouseId, from, to)) {
            if (CONSUMPTION_TYPES.contains(movement.type())) {
                consumption.merge(movement.stockItemId(), movement.value().negate(), BigDecimal::add);
            }
        }

        Map<Long, StockItemDTO> items = items();
        Set<Long> itemIds = new TreeSet<>(opening.keySet());
        itemIds.addAll(closing.keySet());
        itemIds.addAll(consumption.keySet());
        List<RotationLine> lines = new ArrayList<>();
        BigDecimal totalConsumption = BigDecimal.ZERO;
        BigDecimal totalOpening = BigDecimal.ZERO;
        BigDecimal totalClosing = BigDecimal.ZERO;
        for (Long itemId : itemIds) {
            BigDecimal open = money(opening.getOrDefault(itemId, BigDecimal.ZERO));
            BigDecimal close = money(closing.getOrDefault(itemId, BigDecimal.ZERO));
            BigDecimal used = money(consumption.getOrDefault(itemId, BigDecimal.ZERO));
            if (open.signum() == 0 && close.signum() == 0 && used.signum() == 0) {
                continue;
            }
            ReportMath.Rotation rotation = ReportMath.rotation(open, close, used, days);
            StockItemDTO item = items.get(itemId);
            lines.add(new RotationLine(itemId, item.code(), item.name(), item.itemCategory(), open, close,
                    rotation.averageValue(), used, rotation.rotation(), rotation.coverageDays()));
            totalConsumption = totalConsumption.add(used);
            totalOpening = totalOpening.add(open);
            totalClosing = totalClosing.add(close);
        }
        // Primero lo que no se mueve: sin consumo y después la menor rotación
        lines.sort(Comparator.comparing(RotationLine::rotation, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(Comparator.comparing(RotationLine::averageValue).reversed()));
        ReportMath.Rotation total = ReportMath.rotation(totalOpening, totalClosing, totalConsumption, days);
        return new RotationReport(from, to, warehouseId, days, lines, totalConsumption, total.averageValue(),
                total.rotation(), total.coverageDays());
    }

    @Override
    public SupplierPriceReport supplierPrices(Long stockItemId, Long supplierId, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("La fecha 'from' no puede ser posterior a 'to'");
        }
        List<StockItemDTO> items = stockItemId != null
                ? List.of(stockItemService.getStockItem(stockItemId))
                : stockItemService.getStockItems(true, null);
        UnitConverter generic = unitService.genericConverter();

        List<PriceSeries> series = new ArrayList<>();
        for (StockItemDTO item : items) {
            UnitConverter converter = StockItemUnits.converterFor(item, generic);
            Map<Long, List<PurchasePriceDTO>> bySupplier = stockItemService.getPriceHistory(item.id()).stream()
                    .filter(price -> supplierId == null || supplierId.equals(price.supplierId()))
                    .filter(price -> from == null || !price.purchasedOn().isBefore(from))
                    .filter(price -> to == null || !price.purchasedOn().isAfter(to))
                    .filter(price -> price.unit().equals(item.stockUnit())
                            || converter.canConvert(price.unit(), item.stockUnit()))
                    .collect(Collectors.groupingBy(PurchasePriceDTO::supplierId, TreeMap::new, Collectors.toList()));
            for (List<PurchasePriceDTO> prices : bySupplier.values()) {
                // El histórico llega del más reciente al más antiguo
                List<PricePoint> points = prices.reversed().stream()
                        .map(price -> new PricePoint(price.purchasedOn(),
                                perStockUnit(price, item, converter), price.reference()))
                        .toList();
                BigDecimal first = points.getFirst().unitPrice();
                BigDecimal last = points.getLast().unitPrice();
                BigDecimal min = points.stream().map(PricePoint::unitPrice).min(Comparator.naturalOrder()).orElseThrow();
                BigDecimal max = points.stream().map(PricePoint::unitPrice).max(Comparator.naturalOrder()).orElseThrow();
                series.add(new PriceSeries(item.id(), item.code(), item.name(), prices.getFirst().supplierId(),
                        prices.getFirst().supplierName(), item.stockUnit(), points, first, last, min, max,
                        pct(last.subtract(first), first)));
            }
        }
        // Las mayores subidas, primero
        series.sort(Comparator.comparing((PriceSeries s) -> s.changePct() == null ? BigDecimal.ZERO : s.changePct())
                .reversed().thenComparing(PriceSeries::stockItemName));
        return new SupplierPriceReport(from, to, series);
    }

    @Override
    public MenuEngineeringReport menuEngineering(Instant from, Instant to, CurrentUser requester) {
        checkPeriod(from, to);
        List<SoldLine> sales = orderService.getDeliveredSales(from, to);
        Map<Long, ProductDTO> products = products();
        List<RecipeDTO> dishes = activeDishes(requester);
        Set<Long> withRecipe = dishes.stream().map(RecipeDTO::productId).collect(Collectors.toSet());

        Map<Long, Long> sold = new HashMap<>();
        Map<Long, BigDecimal> revenue = new HashMap<>();
        for (SoldLine line : sales) {
            if (line.productId() != null) {
                sold.merge(line.productId(), (long) line.quantity(), Long::sum);
                revenue.merge(line.productId(), line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())),
                        BigDecimal::add);
            }
        }
        List<String> withoutRecipe = sales.stream()
                .filter(line -> line.productId() == null || !withRecipe.contains(line.productId()))
                .map(SoldLine::productName)
                .distinct()
                .sorted()
                .toList();

        record Row(RecipeDTO recipe, ProductDTO product, long sold, BigDecimal netPrice, BigDecimal cost,
                   BigDecimal unitMargin) {
        }
        // Un producto archivado sale de la matriz aunque su escandallo siga activo
        List<Row> rows = dishes.stream().filter(recipe -> products.containsKey(recipe.productId())).map(recipe -> {
            ProductDTO product = products.get(recipe.productId());
            long units = sold.getOrDefault(recipe.productId(), 0L);
            // Precio medio cobrado en el periodo; sin ventas, el de la carta
            BigDecimal gross = units > 0
                    ? revenue.get(recipe.productId()).divide(BigDecimal.valueOf(units), 4, RoundingMode.HALF_UP)
                    : product.getPrice();
            BigDecimal netPrice = net(gross, vatOf(product)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal cost = recipe.costPerPortion() == null ? BigDecimal.ZERO : recipe.costPerPortion();
            return new Row(recipe, product, units, netPrice, cost, netPrice.subtract(cost));
        }).toList();

        long totalSold = rows.stream().mapToLong(Row::sold).sum();
        MenuEngineering.Thresholds thresholds = MenuEngineering.thresholds(rows.stream()
                .map(row -> new MenuEngineering.Input(row.sold(), row.unitMargin()))
                .toList());
        List<MenuItem> items = rows.stream().map(row -> {
            BigDecimal mix = MenuEngineering.mixPct(row.sold(), totalSold);
            return new MenuItem(row.product().getId(), row.product().getName(), row.product().getCategory(), row.sold(),
                    mix, row.netPrice(), money(row.cost()), row.recipe().costComplete(), money(row.unitMargin()),
                    money(row.unitMargin().multiply(BigDecimal.valueOf(row.sold()))), pct(row.cost(), row.netPrice()),
                    MenuEngineering.classify(mix, row.unitMargin(), thresholds));
        }).sorted(Comparator.comparing(MenuItem::totalMargin).reversed().thenComparing(MenuItem::productName))
                .toList();
        return new MenuEngineeringReport(from, to, totalSold, thresholds.popularityPct(), thresholds.margin(), items,
                withoutRecipe);
    }

    private List<FoodCostPoint> series(Instant from, Instant to, int stepDays, Long warehouseId,
                                       List<SoldLine> sales, Map<Long, ProductDTO> products) {
        List<FoodCostPoint> points = new ArrayList<>();
        for (Instant start = from; start.isBefore(to); start = start.plus(stepDays, ChronoUnit.DAYS)) {
            Instant end = start.plus(stepDays, ChronoUnit.DAYS);
            Instant bucketEnd = end.isAfter(to) ? to : end;
            Instant bucketStart = start;
            BigDecimal revenue = sales.stream()
                    .filter(line -> !line.deliveredAt().isBefore(bucketStart) && line.deliveredAt().isBefore(bucketEnd))
                    .map(line -> net(line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())),
                            vatOf(products.get(line.productId()))))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<MovementType, BigDecimal> byType =
                    valueByType(inventoryService.getMovementTotals(warehouseId, bucketStart, bucketEnd));
            BigDecimal theoretical = consumed(byType, MovementType.SALE);
            BigDecimal real = theoretical.add(consumed(byType, MovementType.WASTE))
                    .add(consumed(byType, MovementType.ADJUSTMENT))
                    .add(consumed(byType, MovementType.PRODUCTION_CONSUMPTION))
                    .add(consumed(byType, MovementType.PRODUCTION_OUTPUT));
            points.add(new FoodCostPoint(bucketStart, money(revenue), money(theoretical), money(real),
                    pct(theoretical, revenue), pct(real, revenue)));
        }
        return points;
    }

    private static void checkPeriod(Instant from, Instant to) {
        if (!from.isBefore(to)) {
            throw new BadRequestException("La fecha 'from' debe ser anterior a 'to'");
        }
    }

    private static Map<MovementType, BigDecimal> valueByType(List<MovementTotalDTO> totals) {
        Map<MovementType, BigDecimal> byType = new EnumMap<>(MovementType.class);
        totals.forEach(total -> byType.merge(total.type(), total.value(), BigDecimal::add));
        return byType;
    }

    private static Map<Long, BigDecimal> valueByItem(List<MovementTotalDTO> totals) {
        Map<Long, BigDecimal> byItem = new HashMap<>();
        totals.forEach(total -> byItem.merge(total.stockItemId(), total.value(), BigDecimal::add));
        return byItem;
    }

    /** Valor que ha salido del stock por ese tipo de movimiento (las salidas tienen valor negativo). */
    private static BigDecimal consumed(Map<MovementType, BigDecimal> byType, MovementType type) {
        return byType.getOrDefault(type, BigDecimal.ZERO).negate();
    }

    private static BigDecimal perStockUnit(PurchasePriceDTO price, StockItemDTO item, UnitConverter converter) {
        if (price.unit().equals(item.stockUnit())) {
            return price.unitPrice().setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal stockUnitsPerPurchaseUnit = converter.convert(BigDecimal.ONE, price.unit(), item.stockUnit());
        return price.unitPrice().divide(stockUnitsPerPurchaseUnit, 4, RoundingMode.HALF_UP);
    }

    private static BigDecimal vatOf(ProductDTO product) {
        return product == null ? null : product.getVatRate();
    }

    private Map<Long, StockItemDTO> items() {
        return stockItemService.getStockItems(null, null).stream()
                .collect(Collectors.toMap(StockItemDTO::id, Function.identity()));
    }

    private Map<Long, ProductDTO> products() {
        return productService.getAllProducts().stream()
                .collect(Collectors.toMap(ProductDTO::getId, Function.identity()));
    }

    private List<RecipeDTO> activeDishes(CurrentUser requester) {
        return recipeService.getRecipes(RecipeType.DISH, requester).stream()
                .filter(recipe -> Boolean.TRUE.equals(recipe.active()) && recipe.productId() != null)
                .toList();
    }
}
