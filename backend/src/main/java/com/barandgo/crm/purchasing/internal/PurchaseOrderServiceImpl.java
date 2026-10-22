package com.barandgo.crm.purchasing.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.masterdata.SupplierDTO;
import com.barandgo.crm.masterdata.SupplierService;
import com.barandgo.crm.masterdata.UnitConverter;
import com.barandgo.crm.masterdata.VatRateService;
import com.barandgo.crm.purchasing.PurchaseOrderDTO;
import com.barandgo.crm.purchasing.PurchaseOrderLineRequestDTO;
import com.barandgo.crm.purchasing.PurchaseOrderRequestDTO;
import com.barandgo.crm.purchasing.PurchaseOrderService;
import com.barandgo.crm.purchasing.PurchaseOrderStatus;
import com.barandgo.crm.shared.BadRequestException;
import com.barandgo.crm.shared.ConflictException;
import com.barandgo.crm.shared.ResourceNotFoundException;

@Service
@Transactional
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseOrderServiceImpl.class);

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final StockItemRepository stockItemRepository;
    private final StockItemSupplierRepository supplierPriceRepository;
    private final UnitCatalog unitCatalog;
    private final SupplierService supplierService;
    private final VatRateService vatRateService;

    public PurchaseOrderServiceImpl(PurchaseOrderRepository purchaseOrderRepository,
                                    StockItemRepository stockItemRepository,
                                    StockItemSupplierRepository supplierPriceRepository,
                                    UnitCatalog unitCatalog,
                                    SupplierService supplierService,
                                    VatRateService vatRateService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.stockItemRepository = stockItemRepository;
        this.supplierPriceRepository = supplierPriceRepository;
        this.unitCatalog = unitCatalog;
        this.supplierService = supplierService;
        this.vatRateService = vatRateService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrderDTO> getPurchaseOrders(PurchaseOrderStatus status, Long supplierId) {
        UnitCatalog.Units units = unitCatalog.load();
        Map<Long, String> supplierNames = supplierService.getSuppliers(null).stream()
                .collect(Collectors.toMap(SupplierDTO::id, SupplierDTO::legalName));

        return purchaseOrderRepository.findAllByOrderByIdDesc().stream()
                .filter(order -> status == null || status == order.getStatus())
                .filter(order -> supplierId == null || supplierId.equals(order.getSupplierId()))
                .map(order -> toDTO(order, units, supplierNames.get(order.getSupplierId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderDTO getPurchaseOrder(Long id) {
        PurchaseOrder order = findOrder(id);
        return toDTO(order, unitCatalog.load(), supplierService.getSupplierById(order.getSupplierId()).legalName());
    }

    @Override
    public PurchaseOrderDTO createPurchaseOrder(PurchaseOrderRequestDTO request, CurrentUser requester) {
        SupplierDTO supplier = activeSupplier(request.supplierId());

        PurchaseOrder order = new PurchaseOrder();
        LocalDate today = LocalDate.now();
        order.setNumber(nextNumber(today));
        order.setOrderDate(today);
        order.setCreatedBy(requester.id());
        applyHeader(order, request, supplier);

        UnitCatalog.Units units = unitCatalog.load();
        addLines(order, request.lines(), supplier, units);

        PurchaseOrder saved = purchaseOrderRepository.save(order);
        logger.info("Pedido a proveedor {} creado para el proveedor {}", saved.getNumber(), supplier.id());
        return toDTO(saved, units, supplier.legalName());
    }

    @Override
    public PurchaseOrderDTO updatePurchaseOrder(Long id, PurchaseOrderRequestDTO request) {
        PurchaseOrder order = findOrder(id);
        order.ensureEditable();
        SupplierDTO supplier = activeSupplier(request.supplierId());
        applyHeader(order, request, supplier);

        // Las líneas se sustituyen: primero se borran en BD para no chocar con las claves únicas
        // (pedido, artículo) y (pedido, nº de línea), porque Hibernate inserta antes de borrar
        order.getLines().clear();
        purchaseOrderRepository.saveAndFlush(order);

        UnitCatalog.Units units = unitCatalog.load();
        addLines(order, request.lines(), supplier, units);

        logger.info("Pedido a proveedor {} actualizado", order.getNumber());
        return toDTO(purchaseOrderRepository.save(order), units, supplier.legalName());
    }

    @Override
    public PurchaseOrderDTO sendPurchaseOrder(Long id) {
        PurchaseOrder order = findOrder(id);
        SupplierDTO supplier = activeSupplier(order.getSupplierId());
        order.send();

        logger.info("Pedido a proveedor {} enviado", order.getNumber());
        return toDTO(purchaseOrderRepository.save(order), unitCatalog.load(), supplier.legalName());
    }

    @Override
    public PurchaseOrderDTO cancelPurchaseOrder(Long id) {
        PurchaseOrder order = findOrder(id);
        order.cancel();

        logger.info("Pedido a proveedor {} cancelado", order.getNumber());
        return toDTO(purchaseOrderRepository.save(order), unitCatalog.load(),
                supplierService.getSupplierById(order.getSupplierId()).legalName());
    }

    // Un borrador no ha salido del local: se puede borrar. A partir de ahí solo se cancela
    @Override
    public void deletePurchaseOrder(Long id) {
        PurchaseOrder order = findOrder(id);
        if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new ConflictException("Solo se puede borrar un pedido en borrador; este se puede cancelar");
        }
        purchaseOrderRepository.delete(order);
        logger.info("Pedido a proveedor {} borrado", order.getNumber());
    }

    private void applyHeader(PurchaseOrder order, PurchaseOrderRequestDTO request, SupplierDTO supplier) {
        LocalDate expected = request.expectedDate();
        if (expected == null && supplier.leadTimeDays() != null) {
            expected = order.getOrderDate().plusDays(supplier.leadTimeDays());
        }
        if (expected != null && expected.isBefore(order.getOrderDate())) {
            throw new BadRequestException("La fecha de entrega prevista no puede ser anterior a la del pedido");
        }
        order.setSupplierId(supplier.id());
        order.setExpectedDate(expected);
        order.setNotes(request.notes());
    }

    private void addLines(PurchaseOrder order, List<PurchaseOrderLineRequestDTO> requests, SupplierDTO supplier,
                          UnitCatalog.Units units) {
        Set<Long> seen = new HashSet<>();
        int lineNumber = 1;
        for (PurchaseOrderLineRequestDTO request : requests) {
            if (!seen.add(request.stockItemId())) {
                throw new BadRequestException("El artículo " + request.stockItemId()
                        + " aparece en más de una línea; indica la cantidad total en una sola");
            }
            order.addLine(buildLine(request, lineNumber++, supplier, units));
        }
    }

    /**
     * Unidad y precio, si no se indican, salen de la tarifa del artículo en ese proveedor. Si se pide
     * en otra unidad que la de tarifa, el precio se convierte.
     */
    private PurchaseOrderLine buildLine(PurchaseOrderLineRequestDTO request, int lineNumber, SupplierDTO supplier,
                                        UnitCatalog.Units units) {
        StockItem item = stockItemRepository.findById(request.stockItemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Artículo de compra no encontrado con ID: " + request.stockItemId()));
        if (!Boolean.TRUE.equals(item.getActive())) {
            throw new BadRequestException("El artículo '" + item.getName() + "' está desactivado");
        }

        Optional<StockItemSupplier> tariff = supplierPriceRepository.findByStockItemIdAndSupplierId(
                item.getId(), supplier.id());
        UnitConverter converter = units.converterFor(item.getConversions());

        String unit = request.unit() != null ? request.unit()
                : tariff.map(t -> units.codeOf(t.getPurchaseUnitId()))
                        .orElseGet(() -> units.codeOf(item.getPurchaseUnitId()));
        Long unitId = units.idOf(unit);
        String stockUnit = units.codeOf(item.getStockUnitId());
        if (!converter.canConvert(unit, stockUnit)) {
            throw new BadRequestException("El artículo '" + item.getName() + "' no se puede pedir en '" + unit
                    + "': no hay conversión a su unidad de inventario '" + stockUnit + "'");
        }

        BigDecimal unitPrice = request.unitPrice();
        if (unitPrice == null) {
            StockItemSupplier price = tariff.orElseThrow(() -> new BadRequestException("El artículo '"
                    + item.getName() + "' no tiene precio para " + supplier.legalName() + ": indica unitPrice"));
            String tariffUnit = units.codeOf(price.getPurchaseUnitId());
            unitPrice = PurchaseOrderTotals.priceInOrderedUnit(price.getUnitPrice(), converter.factor(unit, tariffUnit));
        }

        PurchaseOrderLine line = new PurchaseOrderLine();
        line.setLineNumber(lineNumber);
        line.setStockItem(item);
        line.setUnitId(unitId);
        line.setQuantity(request.quantity());
        line.setUnitPrice(unitPrice);
        line.setVatRate(vatRateService.getVatRate(item.getVatRateId()).rate());
        return line;
    }

    private SupplierDTO activeSupplier(Long supplierId) {
        SupplierDTO supplier = supplierService.getSupplierById(supplierId);
        if (!Boolean.TRUE.equals(supplier.active())) {
            throw new BadRequestException("El proveedor '" + supplier.legalName() + "' está desactivado");
        }
        return supplier;
    }

    private String nextNumber(LocalDate date) {
        return "PC-%d-%05d".formatted(date.getYear(), purchaseOrderRepository.nextNumberSequence());
    }

    private PurchaseOrder findOrder(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido a proveedor no encontrado con ID: " + id));
    }

    private static PurchaseOrderDTO toDTO(PurchaseOrder order, UnitCatalog.Units units, String supplierName) {
        PurchaseOrderTotals.Result totals = PurchaseOrderTotals.calculate(order.getLines().stream()
                .map(line -> new PurchaseOrderTotals.Line(line.getQuantity(), line.getUnitPrice(), line.getVatRate()))
                .toList());

        List<PurchaseOrderDTO.Line> lines = new ArrayList<>();
        for (int i = 0; i < order.getLines().size(); i++) {
            PurchaseOrderLine line = order.getLines().get(i);
            StockItem item = line.getStockItem();
            lines.add(new PurchaseOrderDTO.Line(line.getId(), line.getLineNumber(), item.getId(), item.getCode(),
                    item.getName(), units.codeOf(line.getUnitId()), line.getQuantity(), line.getUnitPrice(),
                    line.getVatRate(), totals.lineAmounts().get(i)));
        }
        List<PurchaseOrderDTO.VatBreakdown> breakdown = totals.vatGroups().stream()
                .map(group -> new PurchaseOrderDTO.VatBreakdown(group.vatRate(), group.base(), group.vat()))
                .toList();

        return new PurchaseOrderDTO(order.getId(), order.getNumber(), order.getSupplierId(), supplierName,
                order.getStatus(), order.getOrderDate(), order.getExpectedDate(), order.getNotes(),
                order.getCreatedBy(), order.getSentAt(), order.getCancelledAt(), lines, breakdown,
                totals.totalBase(), totals.totalVat(), totals.total());
    }
}
