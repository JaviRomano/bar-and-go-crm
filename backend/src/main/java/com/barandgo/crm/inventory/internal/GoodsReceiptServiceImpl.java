package com.barandgo.crm.inventory.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.identity.UserRole;
import com.barandgo.crm.inventory.GoodsReceiptDTOs;
import com.barandgo.crm.inventory.GoodsReceiptDTOs.GoodsReceiptDTO;
import com.barandgo.crm.inventory.GoodsReceiptDTOs.GoodsReceiptRequestDTO;
import com.barandgo.crm.inventory.GoodsReceiptDTOs.LineRequest;
import com.barandgo.crm.inventory.GoodsReceiptService;
import com.barandgo.crm.inventory.MovementSource;
import com.barandgo.crm.inventory.MovementType;
import com.barandgo.crm.masterdata.SupplierDTO;
import com.barandgo.crm.masterdata.SupplierService;
import com.barandgo.crm.masterdata.UnitOfMeasureDTO;
import com.barandgo.crm.masterdata.UnitOfMeasureService;
import com.barandgo.crm.purchasing.PurchaseOrderDTO;
import com.barandgo.crm.purchasing.PurchaseOrderService;
import com.barandgo.crm.purchasing.PurchaseOrderStatus;
import com.barandgo.crm.purchasing.StockItemDTO;
import com.barandgo.crm.purchasing.StockItemService;
import com.barandgo.crm.purchasing.StockItemSupplierDTO;
import com.barandgo.crm.shared.BadRequestException;
import com.barandgo.crm.shared.ConflictException;
import com.barandgo.crm.shared.ForbiddenException;
import com.barandgo.crm.shared.ResourceNotFoundException;

@Service
@Transactional
public class GoodsReceiptServiceImpl implements GoodsReceiptService {

    private static final Logger logger = LoggerFactory.getLogger(GoodsReceiptServiceImpl.class);

    private final GoodsReceiptRepository receiptRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockLotRepository lotRepository;
    private final StockLedger ledger;
    private final ItemCatalog itemCatalog;
    private final SupplierService supplierService;
    private final UnitOfMeasureService unitService;
    private final StockItemService stockItemService;
    private final PurchaseOrderService purchaseOrderService;

    public GoodsReceiptServiceImpl(GoodsReceiptRepository receiptRepository, WarehouseRepository warehouseRepository,
                                   StockLotRepository lotRepository, StockLedger ledger, ItemCatalog itemCatalog, SupplierService supplierService,
                                   UnitOfMeasureService unitService, StockItemService stockItemService,
                                   PurchaseOrderService purchaseOrderService) {
        this.receiptRepository = receiptRepository;
        this.warehouseRepository = warehouseRepository;
        this.lotRepository = lotRepository;
        this.ledger = ledger;
        this.itemCatalog = itemCatalog;
        this.supplierService = supplierService;
        this.unitService = unitService;
        this.stockItemService = stockItemService;
        this.purchaseOrderService = purchaseOrderService;
    }

    @Override
    public GoodsReceiptDTO registerReceipt(GoodsReceiptRequestDTO request, CurrentUser requester) {
        SupplierDTO supplier = supplierService.getSupplierById(request.supplierId());
        if (!Boolean.TRUE.equals(supplier.active())) {
            throw new BadRequestException("El proveedor '" + supplier.legalName() + "' está desactivado");
        }
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .filter(Warehouse::getActive)
                .orElseThrow(() -> new BadRequestException("Almacén inexistente o desactivado: " + request.warehouseId()));
        LocalDate receiptDate = request.receiptDate() != null ? request.receiptDate() : LocalDate.now();
        if (receiptDate.isAfter(LocalDate.now())) {
            throw new BadRequestException("La fecha de recepción no puede ser futura");
        }
        PurchaseOrderDTO order = request.purchaseOrderId() == null ? null : receivableOrder(request.purchaseOrderId(),
                supplier);

        GoodsReceipt receipt = new GoodsReceipt(nextNumber(receiptDate), supplier.id(),
                request.supplierDeliveryNote(), order != null ? order.id() : null, warehouse.getId(), receiptDate,
                request.notes(), requester.id());

        Map<String, Long> unitIds = unitService.getUnits().stream()
                .collect(Collectors.toMap(UnitOfMeasureDTO::code, UnitOfMeasureDTO::id));
        List<ReceivedLine> received = new ArrayList<>();
        int lineNumber = 1;
        for (LineRequest lineRequest : request.lines()) {
            ReceivedLine line = resolveLine(lineRequest, order, supplier, requester);
            Long unitId = unitIds.get(line.unit());
            if (unitId == null) {
                throw new BadRequestException("Unidad de medida desconocida: '" + line.unit() + "'");
            }
            receipt.addLine(new GoodsReceiptLine(lineNumber++, line.orderLine() != null ? line.orderLine().id() : null,
                    line.item().id(), unitId, lineRequest.quantity(), line.unitPrice(), line.stockQuantity(),
                    line.stockUnitCost()));
            received.add(line);
        }
        GoodsReceipt saved = receiptRepository.save(receipt);

        postStockEntries(saved, received, requester);
        registerPurchases(saved, received, order, supplier);

        logger.info("Albarán {} registrado: {} líneas del proveedor {}", saved.getNumber(), received.size(),
                supplier.id());
        return toDTO(saved, supplier.legalName(), warehouse.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public GoodsReceiptDTO getReceipt(Long id) {
        GoodsReceipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Albarán no encontrado con ID: " + id));
        return toDTO(receipt, supplierService.getSupplierById(receipt.getSupplierId()).legalName(),
                warehouseRepository.findById(receipt.getWarehouseId()).map(Warehouse::getName).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoodsReceiptDTO> getReceipts(Long supplierId) {
        Map<Long, String> supplierNames = supplierService.getSuppliers(null).stream()
                .collect(Collectors.toMap(SupplierDTO::id, SupplierDTO::legalName));
        Map<Long, String> warehouseNames = warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getId, Warehouse::getName));
        return receiptRepository.findAllByOrderByIdDesc().stream()
                .filter(receipt -> supplierId == null || supplierId.equals(receipt.getSupplierId()))
                .map(receipt -> toDTO(receipt, supplierNames.get(receipt.getSupplierId()),
                        warehouseNames.get(receipt.getWarehouseId())))
                .toList();
    }

    /** Línea ya resuelta: artículo, unidad, precio y su equivalente en unidad de inventario. */
    private record ReceivedLine(ItemCatalog.Item item, LineRequest request, PurchaseOrderDTO.Line orderLine,
                                String unit, BigDecimal unitPrice, BigDecimal stockQuantity,
                                BigDecimal stockUnitCost) {
    }

    private PurchaseOrderDTO receivableOrder(Long orderId, SupplierDTO supplier) {
        PurchaseOrderDTO order = purchaseOrderService.getPurchaseOrder(orderId);
        if (!order.supplierId().equals(supplier.id())) {
            throw new BadRequestException("El pedido " + order.number() + " es de otro proveedor");
        }
        if (order.status() != PurchaseOrderStatus.SENT && order.status() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new ConflictException("El pedido " + order.number() + " no admite recepciones (estado "
                    + order.status() + ")");
        }
        return order;
    }

    private ReceivedLine resolveLine(LineRequest request, PurchaseOrderDTO order, SupplierDTO supplier,
                                     CurrentUser requester) {
        ItemCatalog.Item item = itemCatalog.get(request.stockItemId());
        StockItemDTO dto = item.dto();
        if (!Boolean.TRUE.equals(dto.active())) {
            throw new BadRequestException("El artículo '" + dto.name() + "' está desactivado");
        }
        if (Boolean.TRUE.equals(dto.perishable()) && (isBlank(request.lotCode()) || request.expiryDate() == null)) {
            throw new BadRequestException("El artículo '" + dto.name() + "' es perecedero: indica lote y fecha de caducidad");
        }

        // Un pedido tiene una sola línea por artículo (regla de compras), así que se cruza por artículo
        PurchaseOrderDTO.Line orderLine = order == null ? null : order.lines().stream()
                .filter(line -> line.stockItemId().equals(dto.id()))
                .findFirst()
                .orElse(null);
        Optional<StockItemSupplierDTO> tariff = orderLine != null ? Optional.empty()
                : stockItemService.getSuppliers(dto.id()).stream()
                        .filter(t -> t.supplierId().equals(supplier.id()))
                        .findFirst();

        String unit = request.unit() != null ? request.unit()
                : orderLine != null ? orderLine.unit()
                : tariff.map(StockItemSupplierDTO::purchaseUnit).orElse(dto.purchaseUnit());

        BigDecimal expectedPrice = orderLine != null
                ? priceIn(item, orderLine.unitPrice(), orderLine.unit(), unit)
                : tariff.map(t -> priceIn(item, t.unitPrice(), t.purchaseUnit(), unit)).orElse(null);
        BigDecimal unitPrice = request.unitPrice() != null ? request.unitPrice() : expectedPrice;
        if (unitPrice == null) {
            throw new BadRequestException("El artículo '" + dto.name() + "' no tiene precio de pedido ni de tarifa: "
                    + "indica unitPrice");
        }
        // Matriz de permisos: el personal recibe, pero un precio distinto lo valida un encargado
        if (requester.role() == UserRole.STAFF && request.unitPrice() != null
                && (expectedPrice == null || expectedPrice.compareTo(request.unitPrice()) != 0)) {
            throw new ForbiddenException("Solo un encargado puede registrar un precio distinto del pedido o la tarifa");
        }

        BigDecimal stockQuantity = item.toStockUnit(request.quantity(), unit);
        if (stockQuantity.signum() <= 0) {
            throw new BadRequestException("La cantidad recibida de '" + dto.name() + "' es demasiado pequeña");
        }
        BigDecimal stockUnitCost = request.quantity().multiply(unitPrice)
                .divide(stockQuantity, MovingAverage.COST_SCALE, RoundingMode.HALF_UP);
        return new ReceivedLine(item, request, orderLine, unit, unitPrice, stockQuantity, stockUnitCost);
    }

    /** Precio expresado en otra unidad: 12 € la caja de 24 son 0,50 € la unidad. */
    private static BigDecimal priceIn(ItemCatalog.Item item, BigDecimal price, String priceUnit, String targetUnit) {
        if (priceUnit.equals(targetUnit)) {
            return price;
        }
        return price.multiply(item.converter().factor(targetUnit, priceUnit)).setScale(4, RoundingMode.HALF_UP);
    }

    private void postStockEntries(GoodsReceipt receipt, List<ReceivedLine> received, CurrentUser requester) {
        List<StockLedger.Entry> entries = new ArrayList<>();
        for (int i = 0; i < received.size(); i++) {
            ReceivedLine line = received.get(i);
            GoodsReceiptLine saved = receipt.getLines().get(i);
            StockLedger.NewLot lot = isBlank(line.request().lotCode()) ? null
                    : new StockLedger.NewLot(line.request().lotCode().trim(), line.request().expiryDate());
            entries.add(new StockLedger.Entry(MovementType.PURCHASE_RECEIPT, line.item().id(), receipt.getWarehouseId(),
                    line.stockQuantity(), line.stockUnitCost(), MovementSource.GOODS_RECEIPT, receipt.getId(),
                    saved.getId(), lot, "Albarán " + receipt.getNumber()));
        }
        List<StockLedger.Posted> posted = ledger.post(UUID.randomUUID(), entries, requester.id());
        for (int i = 0; i < posted.size(); i++) {
            StockLot lot = posted.get(i).createdLot();
            if (lot != null) {
                receipt.getLines().get(i).setLotId(lot.getId());
            }
        }
    }

    private void registerPurchases(GoodsReceipt receipt, List<ReceivedLine> received, PurchaseOrderDTO order,
                                   SupplierDTO supplier) {
        for (ReceivedLine line : received) {
            stockItemService.recordPurchasePrice(line.item().id(), supplier.id(), line.unit(), line.unitPrice(),
                    receipt.getReceiptDate(), receipt.getNumber());
        }
        if (order == null) {
            return;
        }
        // Lo recibido se devuelve al pedido en la unidad de cada línea del pedido
        Map<Long, BigDecimal> receivedByLine = new HashMap<>();
        for (ReceivedLine line : received) {
            if (line.orderLine() == null) {
                continue;
            }
            BigDecimal inOrderUnit = line.orderLine().unit().equals(line.unit())
                    ? line.request().quantity()
                    : line.item().converter().convert(line.request().quantity(), line.unit(), line.orderLine().unit());
            receivedByLine.merge(line.orderLine().id(), inOrderUnit.setScale(4, RoundingMode.HALF_UP), BigDecimal::add);
        }
        if (!receivedByLine.isEmpty()) {
            purchaseOrderService.registerReceipt(order.id(), receivedByLine);
        }
    }

    private String nextNumber(LocalDate date) {
        return "AL-%d-%05d".formatted(date.getYear(), receiptRepository.nextNumberSequence());
    }

    private GoodsReceiptDTO toDTO(GoodsReceipt receipt, String supplierName, String warehouseName) {
        Map<Long, StockItemDTO> items = itemCatalog.all();
        Map<Long, String> unitCodes = unitService.getUnits().stream()
                .collect(Collectors.toMap(UnitOfMeasureDTO::id, UnitOfMeasureDTO::code));
        Map<Long, StockLot> lots = lotRepository.findAllById(receipt.getLines().stream()
                        .map(GoodsReceiptLine::getLotId).filter(id -> id != null).toList())
                .stream().collect(Collectors.toMap(StockLot::getId, lot -> lot));
        BigDecimal total = BigDecimal.ZERO;
        List<GoodsReceiptDTOs.Line> lines = new ArrayList<>();
        for (GoodsReceiptLine line : receipt.getLines()) {
            StockItemDTO item = items.get(line.getStockItemId());
            StockLot lot = lots.get(line.getLotId());
            BigDecimal amount = line.getQuantity().multiply(line.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
            total = total.add(amount);
            lines.add(new GoodsReceiptDTOs.Line(line.getId(), line.getLineNumber(), line.getPurchaseOrderLineId(),
                    item.id(), item.code(), item.name(), unitCodes.get(line.getUnitId()), line.getQuantity(),
                    line.getUnitPrice(), amount, line.getStockQuantity(), item.stockUnit(), line.getStockUnitCost(),
                    lot != null ? lot.getLotCode() : null, lot != null ? lot.getExpiryDate() : null));
        }
        return new GoodsReceiptDTO(receipt.getId(), receipt.getNumber(), receipt.getSupplierId(), supplierName,
                receipt.getSupplierDeliveryNote(), receipt.getPurchaseOrderId(), receipt.getWarehouseId(), warehouseName,
                receipt.getReceiptDate(), receipt.getNotes(), receipt.getReceivedBy(), lines, total);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
