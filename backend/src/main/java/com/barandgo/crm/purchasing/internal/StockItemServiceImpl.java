package com.barandgo.crm.purchasing.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barandgo.crm.masterdata.Allergen;
import com.barandgo.crm.masterdata.ItemCategoryDTO;
import com.barandgo.crm.masterdata.ItemCategoryService;
import com.barandgo.crm.masterdata.SupplierDTO;
import com.barandgo.crm.masterdata.SupplierService;
import com.barandgo.crm.masterdata.UnitConverter;
import com.barandgo.crm.masterdata.VatRateDTO;
import com.barandgo.crm.masterdata.VatRateService;
import com.barandgo.crm.purchasing.ItemUnitConversionDTO;
import com.barandgo.crm.purchasing.PurchasePriceDTO;
import com.barandgo.crm.purchasing.StockItemDTO;
import com.barandgo.crm.purchasing.StockItemService;
import com.barandgo.crm.purchasing.StockItemSupplierDTO;
import com.barandgo.crm.purchasing.StockItemUsageCheck;
import com.barandgo.crm.shared.BadRequestException;
import com.barandgo.crm.shared.ConflictException;
import com.barandgo.crm.shared.ResourceNotFoundException;

@Service
@Transactional
public class StockItemServiceImpl implements StockItemService {

    private static final Logger logger = LoggerFactory.getLogger(StockItemServiceImpl.class);

    private final StockItemRepository stockItemRepository;
    private final StockItemSupplierRepository supplierPriceRepository;
    private final PurchasePriceHistoryRepository priceHistoryRepository;
    private final UnitCatalog unitCatalog;
    private final ItemCategoryService itemCategoryService;
    private final VatRateService vatRateService;
    private final SupplierService supplierService;
    private final StockItemUsageCheck usageCheck;

    public StockItemServiceImpl(StockItemRepository stockItemRepository,
                                StockItemSupplierRepository supplierPriceRepository,
                                PurchasePriceHistoryRepository priceHistoryRepository,
                                UnitCatalog unitCatalog,
                                ItemCategoryService itemCategoryService,
                                VatRateService vatRateService,
                                SupplierService supplierService,
                                StockItemUsageCheck usageCheck) {
        this.stockItemRepository = stockItemRepository;
        this.supplierPriceRepository = supplierPriceRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.unitCatalog = unitCatalog;
        this.itemCategoryService = itemCategoryService;
        this.vatRateService = vatRateService;
        this.supplierService = supplierService;
        this.usageCheck = usageCheck;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockItemDTO> getStockItems(Boolean active, Long itemCategoryId) {
        UnitCatalog.Units units = unitCatalog.load();
        Map<Long, String> categoryNames = itemCategoryService.getCategories().stream()
                .collect(Collectors.toMap(ItemCategoryDTO::id, ItemCategoryDTO::name));

        return stockItemRepository.findAllByOrderByNameAsc().stream()
                .filter(item -> active == null || active.equals(item.getActive()))
                .filter(item -> itemCategoryId == null || itemCategoryId.equals(item.getItemCategoryId()))
                .map(item -> toDTO(item, units, categoryNames.get(item.getItemCategoryId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StockItemDTO getStockItem(Long id) {
        StockItem item = findItem(id);
        String categoryName = itemCategoryService.getCategory(item.getItemCategoryId()).name();
        return toDTO(item, unitCatalog.load(), categoryName);
    }

    @Override
    public StockItemDTO createStockItem(StockItemDTO dto) {
        String code = normalizeCode(dto.code());
        if (stockItemRepository.existsByCode(code)) {
            throw new ConflictException("Ya existe un artículo con el código " + code);
        }

        StockItem item = new StockItem();
        item.setCode(code);
        UnitCatalog.Units units = unitCatalog.load();
        String categoryName = apply(item, dto, units);
        item.setActive(dto.active() != null ? dto.active() : true);

        StockItem saved = stockItemRepository.save(item);
        logger.info("Artículo de compra {} ({}) creado", saved.getId(), saved.getCode());
        return toDTO(saved, units, categoryName);
    }

    @Override
    public StockItemDTO updateStockItem(Long id, StockItemDTO dto) {
        StockItem item = findItem(id);
        String code = normalizeCode(dto.code());
        if (stockItemRepository.existsByCodeAndIdNot(code, id)) {
            throw new ConflictException("Ya existe otro artículo con el código " + code);
        }

        item.setCode(code);
        UnitCatalog.Units units = unitCatalog.load();
        // Todos los movimientos de stock están en la unidad de inventario: con movimientos ya no cambia
        if (!units.idOf(dto.stockUnit()).equals(item.getStockUnitId()) && usageCheck.hasStockMovements(id)) {
            throw new ConflictException("El artículo " + item.getCode() + " ya tiene movimientos de stock en '"
                    + units.codeOf(item.getStockUnitId()) + "': su unidad de inventario no se puede cambiar");
        }
        String categoryName = apply(item, dto, units);
        if (dto.active() != null) {
            item.setActive(dto.active());
        }

        logger.info("Artículo de compra {} actualizado", id);
        return toDTO(stockItemRepository.save(item), units, categoryName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockItemSupplierDTO> getSuppliers(Long stockItemId) {
        findItem(stockItemId);
        UnitCatalog.Units units = unitCatalog.load();
        Map<Long, String> supplierNames = supplierService.getSuppliers(null).stream()
                .collect(Collectors.toMap(SupplierDTO::id, SupplierDTO::legalName));

        return supplierPriceRepository.findByStockItemIdOrderByUnitPriceAsc(stockItemId).stream()
                .map(price -> toDTO(price, units, supplierNames.get(price.getSupplierId())))
                .toList();
    }

    @Override
    public StockItemSupplierDTO setSupplierPrice(Long stockItemId, Long supplierId, StockItemSupplierDTO priceDTO) {
        StockItem item = findItem(stockItemId);
        SupplierDTO supplier = supplierService.getSupplierById(supplierId);
        requireActive(supplier.active(), "El proveedor '" + supplier.legalName() + "' está desactivado");

        UnitCatalog.Units units = unitCatalog.load();
        Long unitId = units.idOf(priceDTO.purchaseUnit());
        requireConvertible(units.converterFor(item.getConversions()), priceDTO.purchaseUnit(),
                units.codeOf(item.getStockUnitId()), "del proveedor");

        StockItemSupplier price = supplierPriceRepository.findByStockItemIdAndSupplierId(stockItemId, supplierId)
                .orElseGet(() -> new StockItemSupplier(item, supplierId));
        price.setSupplierItemCode(priceDTO.supplierItemCode());
        price.setPurchaseUnitId(unitId);
        price.setUnitPrice(priceDTO.unitPrice());

        StockItemSupplier saved = supplierPriceRepository.save(price);
        logger.info("Precio del artículo {} en el proveedor {}: {} por {}",
                stockItemId, supplierId, saved.getUnitPrice(), priceDTO.purchaseUnit());
        return toDTO(saved, units, supplier.legalName());
    }

    @Override
    public void removeSupplier(Long stockItemId, Long supplierId) {
        StockItemSupplier price = supplierPriceRepository.findByStockItemIdAndSupplierId(stockItemId, supplierId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El proveedor " + supplierId + " no tiene precio para el artículo " + stockItemId));
        supplierPriceRepository.delete(price);
        logger.info("Proveedor {} retirado del artículo {}", supplierId, stockItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchasePriceDTO> getPriceHistory(Long stockItemId) {
        findItem(stockItemId);
        UnitCatalog.Units units = unitCatalog.load();
        Map<Long, String> supplierNames = supplierService.getSuppliers(null).stream()
                .collect(Collectors.toMap(SupplierDTO::id, SupplierDTO::legalName));

        return priceHistoryRepository.findByStockItemIdOrderByPurchasedOnDescIdDesc(stockItemId).stream()
                .map(entry -> new PurchasePriceDTO(entry.getSupplierId(), supplierNames.get(entry.getSupplierId()),
                        units.codeOf(entry.getUnitId()), entry.getUnitPrice(), entry.getPurchasedOn(),
                        entry.getReference()))
                .toList();
    }

    @Override
    public void recordPurchasePrice(Long stockItemId, Long supplierId, String unit, BigDecimal unitPrice,
                                    LocalDate purchasedOn, String reference) {
        StockItem item = findItem(stockItemId);
        UnitCatalog.Units units = unitCatalog.load();
        Long unitId = units.idOf(unit);
        priceHistoryRepository.save(new PurchasePriceHistory(stockItemId, supplierId, unitId, unitPrice,
                purchasedOn, reference));

        // El último precio pagado pasa a ser la tarifa, en la unidad en la que ya estuviera
        StockItemSupplier tariff = supplierPriceRepository.findByStockItemIdAndSupplierId(stockItemId, supplierId)
                .orElseGet(() -> {
                    StockItemSupplier created = new StockItemSupplier(item, supplierId);
                    created.setPurchaseUnitId(unitId);
                    return created;
                });
        String tariffUnit = units.codeOf(tariff.getPurchaseUnitId());
        BigDecimal tariffPrice = tariffUnit.equals(unit)
                ? unitPrice
                : PurchaseOrderTotals.priceInOrderedUnit(unitPrice,
                        units.converterFor(item.getConversions()).factor(tariffUnit, unit));
        tariff.setUnitPrice(tariffPrice);
        supplierPriceRepository.save(tariff);
    }

    /**
     * Valida y copia los datos del DTO. Devuelve el nombre de la categoría para la respuesta.
     */
    private String apply(StockItem item, StockItemDTO dto, UnitCatalog.Units units) {
        ItemCategoryDTO category = itemCategoryService.getCategory(dto.itemCategoryId());
        requireActive(category.active(), "La categoría '" + category.name() + "' está desactivada");

        VatRateDTO vatRate = vatRateService.getVatRate(dto.vatRateId());
        requireActive(vatRate.active(), "El tipo de IVA '" + vatRate.name() + "' está desactivado");

        if (dto.preferredSupplierId() != null) {
            SupplierDTO supplier = supplierService.getSupplierById(dto.preferredSupplierId());
            requireActive(supplier.active(), "El proveedor '" + supplier.legalName() + "' está desactivado");
        }

        if (dto.minStock() != null && dto.maxStock() != null && dto.minStock().compareTo(dto.maxStock()) > 0) {
            throw new BadRequestException("El stock mínimo no puede ser mayor que el máximo");
        }

        Long purchaseUnitId = units.idOf(dto.purchaseUnit());
        Long stockUnitId = units.idOf(dto.stockUnit());
        Long usageUnitId = units.idOf(dto.usageUnit());

        Set<ItemUnitConversion> conversions = toConversions(dto.conversions(), units);
        UnitConverter converter = units.converterFor(conversions);
        // Sin estas conversiones no se podría pasar lo comprado o lo usado a stock
        requireConvertible(converter, dto.purchaseUnit(), dto.stockUnit(), "de compra");
        requireConvertible(converter, dto.usageUnit(), dto.stockUnit(), "de uso");

        item.setName(dto.name().trim());
        item.setItemCategoryId(category.id());
        item.setPurchaseUnitId(purchaseUnitId);
        item.setStockUnitId(stockUnitId);
        item.setUsageUnitId(usageUnitId);
        item.setConversions(conversions);
        item.setPreferredSupplierId(dto.preferredSupplierId());
        item.setVatRateId(vatRate.id());
        item.setMinStock(dto.minStock());
        item.setMaxStock(dto.maxStock());
        item.setPerishable(Boolean.TRUE.equals(dto.perishable()));
        item.setAllergens(dto.allergens());
        return category.name();
    }

    private static Set<ItemUnitConversion> toConversions(List<ItemUnitConversionDTO> dtos, UnitCatalog.Units units) {
        Set<ItemUnitConversion> conversions = new HashSet<>();
        if (dtos == null) {
            return conversions;
        }
        Set<String> pairs = new HashSet<>();
        for (ItemUnitConversionDTO dto : dtos) {
            if (dto.fromUnit().equals(dto.toUnit())) {
                throw new BadRequestException("Una conversión necesita dos unidades distintas: " + dto.fromUnit());
            }
            // 1 caja = 24 ud y 1 ud = 0,04 caja describen el mismo par: solo se admite una
            String pair = dto.fromUnit().compareTo(dto.toUnit()) < 0
                    ? dto.fromUnit() + "|" + dto.toUnit()
                    : dto.toUnit() + "|" + dto.fromUnit();
            if (!pairs.add(pair)) {
                throw new BadRequestException("Conversión repetida entre '" + dto.fromUnit() + "' y '" + dto.toUnit() + "'");
            }
            conversions.add(new ItemUnitConversion(units.idOf(dto.fromUnit()), units.idOf(dto.toUnit()), dto.factor()));
        }
        return conversions;
    }

    private static void requireConvertible(UnitConverter converter, String from, String stockUnit, String role) {
        if (!converter.canConvert(from, stockUnit)) {
            throw new BadRequestException("No hay conversión de la unidad " + role + " '" + from
                    + "' a la unidad de inventario '" + stockUnit + "'. Añade una conversión del artículo, p. ej. 1 "
                    + from + " = N " + stockUnit);
        }
    }

    private static void requireActive(Boolean active, String message) {
        if (!Boolean.TRUE.equals(active)) {
            throw new BadRequestException(message);
        }
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private StockItem findItem(Long id) {
        return stockItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Artículo de compra no encontrado con ID: " + id));
    }

    private static StockItemDTO toDTO(StockItem item, UnitCatalog.Units units, String categoryName) {
        List<ItemUnitConversionDTO> conversions = item.getConversions().stream()
                .map(c -> new ItemUnitConversionDTO(units.codeOf(c.getFromUnitId()), units.codeOf(c.getToUnitId()),
                        c.getFactor()))
                .sorted((a, b) -> (a.fromUnit() + a.toUnit()).compareTo(b.fromUnit() + b.toUnit()))
                .toList();
        Set<Allergen> allergens = item.getAllergens().isEmpty()
                ? EnumSet.noneOf(Allergen.class)
                : EnumSet.copyOf(item.getAllergens());

        return new StockItemDTO(item.getId(), item.getCode(), item.getName(), item.getItemCategoryId(), categoryName,
                units.codeOf(item.getPurchaseUnitId()), units.codeOf(item.getStockUnitId()),
                units.codeOf(item.getUsageUnitId()), conversions, item.getPreferredSupplierId(), item.getVatRateId(),
                item.getMinStock(), item.getMaxStock(), item.getPerishable(), allergens, item.getActive());
    }

    private static StockItemSupplierDTO toDTO(StockItemSupplier price, UnitCatalog.Units units, String supplierName) {
        return new StockItemSupplierDTO(price.getSupplierId(), supplierName, price.getSupplierItemCode(),
                units.codeOf(price.getPurchaseUnitId()), price.getUnitPrice(), price.getUpdatedAt());
    }
}
