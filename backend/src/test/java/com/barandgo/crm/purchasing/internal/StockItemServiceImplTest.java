package com.barandgo.crm.purchasing.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.barandgo.crm.masterdata.Allergen;
import com.barandgo.crm.masterdata.ItemCategoryDTO;
import com.barandgo.crm.masterdata.ItemCategoryService;
import com.barandgo.crm.masterdata.SupplierService;
import com.barandgo.crm.masterdata.UnitOfMeasureService;
import com.barandgo.crm.masterdata.VatRateDTO;
import com.barandgo.crm.masterdata.VatRateService;
import com.barandgo.crm.purchasing.ItemUnitConversionDTO;
import com.barandgo.crm.purchasing.StockItemCostChanged;
import com.barandgo.crm.purchasing.StockItemDTO;
import com.barandgo.crm.purchasing.StockItemSupplierDTO;
import com.barandgo.crm.purchasing.StockItemUsageCheck;
import com.barandgo.crm.shared.BadRequestException;
import com.barandgo.crm.shared.ConflictException;

@ExtendWith(MockitoExtension.class)
class StockItemServiceImplTest {

    @Mock
    private StockItemRepository stockItemRepository;

    @Mock
    private StockItemSupplierRepository supplierPriceRepository;

    @Mock
    private PurchasePriceHistoryRepository priceHistoryRepository;

    @Mock
    private UnitOfMeasureService unitService;

    @Mock
    private ItemCategoryService itemCategoryService;

    @Mock
    private VatRateService vatRateService;

    @Mock
    private SupplierService supplierService;

    @Mock
    private StockItemUsageCheck usageCheck;

    @Mock
    private ApplicationEventPublisher events;

    private StockItemServiceImpl service;

    @BeforeEach
    void setUp() {
        PurchasingFixtures.stubUnits(unitService);
        lenient().when(itemCategoryService.getCategory(1L)).thenReturn(new ItemCategoryDTO(1L, "Bebidas", true));
        lenient().when(vatRateService.getVatRate(2L))
                .thenReturn(new VatRateDTO(2L, "Reducido", new BigDecimal("10.00"), true));
        lenient().when(stockItemRepository.save(any(StockItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new StockItemServiceImpl(stockItemRepository, supplierPriceRepository, priceHistoryRepository,
                new UnitCatalog(unitService),
                itemCategoryService, vatRateService, supplierService, usageCheck, events);
    }

    @Test
    void itemBoughtByBoxNeedsAConversionToItsStockUnit() {
        assertThatThrownBy(() -> service.createStockItem(water(List.of())))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("'caja'")
                .hasMessageContaining("'ud'");
        verify(stockItemRepository, never()).save(any());
    }

    @Test
    void itemConversionMakesBoxConvertible() {
        StockItemDTO created = service.createStockItem(
                water(List.of(new ItemUnitConversionDTO("caja", "ud", new BigDecimal("24")))));

        assertThat(created.code()).isEqualTo("AGUA-50");
        assertThat(created.purchaseUnit()).isEqualTo("caja");
        assertThat(created.stockUnit()).isEqualTo("ud");
        assertThat(created.itemCategory()).isEqualTo("Bebidas");
        assertThat(created.conversions()).singleElement()
                .satisfies(c -> assertThat(c.factor()).isEqualByComparingTo("24"));
        assertThat(created.allergens()).isEmpty();
    }

    @Test
    void genericConversionIsEnoughForUsageUnit() {
        // Harina: se compra y se cuenta por kg, en cocina se usa por g (kg -> g es genérica)
        StockItemDTO flour = new StockItemDTO(null, "HARINA", "Harina de trigo", 1L, null, "kg", "kg", "g",
                null, null, 2L, new BigDecimal("5"), new BigDecimal("25"), false, Set.of(Allergen.GLUTEN), null);

        StockItemDTO created = service.createStockItem(flour);

        assertThat(created.usageUnit()).isEqualTo("g");
        assertThat(created.allergens()).containsExactly(Allergen.GLUTEN);
    }

    @Test
    void sameUnitPairCannotBeDefinedTwice() {
        List<ItemUnitConversionDTO> conversions = List.of(
                new ItemUnitConversionDTO("caja", "ud", new BigDecimal("24")),
                new ItemUnitConversionDTO("ud", "caja", new BigDecimal("0.0416")));

        assertThatThrownBy(() -> service.createStockItem(water(conversions)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("repetida");
    }

    @Test
    void codeIsNormalizedAndUnique() {
        when(stockItemRepository.existsByCode("AGUA-50")).thenReturn(true);
        StockItemDTO lowercase = new StockItemDTO(null, " agua-50 ", "Agua", 1L, null, "ud", "ud", "ud",
                null, null, 2L, null, null, null, null, null);

        assertThatThrownBy(() -> service.createStockItem(lowercase)).isInstanceOf(ConflictException.class);
    }

    @Test
    void inactiveCategoryIsRejected() {
        when(itemCategoryService.getCategory(9L)).thenReturn(new ItemCategoryDTO(9L, "Antigua", false));
        StockItemDTO item = new StockItemDTO(null, "X", "X", 9L, null, "ud", "ud", "ud",
                null, null, 2L, null, null, null, null, null);

        assertThatThrownBy(() -> service.createStockItem(item))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("desactivada");
    }

    @Test
    void minStockCannotExceedMaxStock() {
        StockItemDTO item = new StockItemDTO(null, "X", "X", 1L, null, "ud", "ud", "ud",
                null, null, 2L, new BigDecimal("10"), new BigDecimal("5"), null, null, null);

        assertThatThrownBy(() -> service.createStockItem(item))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("mínimo");
    }

    @Test
    void supplierPriceUnitMustConvertToStockUnit() {
        when(stockItemRepository.findById(7L)).thenReturn(Optional.of(PurchasingFixtures.bottledWater(7L)));
        when(supplierService.getSupplierById(3L)).thenReturn(PurchasingFixtures.supplier(3L, true, null));

        StockItemSupplierDTO perLiter = new StockItemSupplierDTO(null, null, null, "l", new BigDecimal("1.20"), null);

        assertThatThrownBy(() -> service.setSupplierPrice(7L, 3L, perLiter))
                .isInstanceOf(BadRequestException.class);
        verify(supplierPriceRepository, never()).save(any());
    }

    @Test
    void supplierPriceIsCreatedForConvertibleUnit() {
        when(stockItemRepository.findById(7L)).thenReturn(Optional.of(PurchasingFixtures.bottledWater(7L)));
        when(supplierService.getSupplierById(3L)).thenReturn(PurchasingFixtures.supplier(3L, true, null));
        when(supplierPriceRepository.findByStockItemIdAndSupplierId(7L, 3L)).thenReturn(Optional.empty());
        when(supplierPriceRepository.save(any(StockItemSupplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockItemSupplierDTO saved = service.setSupplierPrice(7L, 3L,
                new StockItemSupplierDTO(null, null, "AG50-24", "caja", new BigDecimal("7.20"), null));

        assertThat(saved.supplierId()).isEqualTo(3L);
        assertThat(saved.purchaseUnit()).isEqualTo("caja");
        assertThat(saved.unitPrice()).isEqualByComparingTo("7.20");
    }

    @Test
    void stockUnitCannotChangeOnceThereAreMovements() {
        when(stockItemRepository.findById(7L)).thenReturn(Optional.of(PurchasingFixtures.bottledWater(7L)));
        when(usageCheck.hasStockMovements(7L)).thenReturn(true);
        StockItemDTO countedByBox = new StockItemDTO(null, "AGUA-50", "Agua 50 cl", 1L, null, "caja", "caja", "caja",
                null, null, 2L, null, null, false, null, null);

        assertThatThrownBy(() -> service.updateStockItem(7L, countedByBox))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("unidad de inventario");
    }

    @Test
    void stockUnitCanChangeWhileThereAreNoMovements() {
        when(stockItemRepository.findById(7L)).thenReturn(Optional.of(PurchasingFixtures.bottledWater(7L)));
        when(usageCheck.hasStockMovements(7L)).thenReturn(false);
        StockItemDTO countedByBox = new StockItemDTO(null, "AGUA-50", "Agua 50 cl", 1L, null, "caja", "caja", "caja",
                null, null, 2L, null, null, false, null, null);

        assertThat(service.updateStockItem(7L, countedByBox).stockUnit()).isEqualTo("caja");
    }

    @Test
    void purchasePriceIsRecordedAndBecomesTariffInItsUnit() {
        StockItem water = PurchasingFixtures.bottledWater(7L);
        StockItemSupplier tariff = new StockItemSupplier(water, 3L);
        tariff.setPurchaseUnitId(PurchasingFixtures.CAJA);
        tariff.setUnitPrice(new BigDecimal("7.20"));
        when(stockItemRepository.findById(7L)).thenReturn(Optional.of(water));
        when(supplierPriceRepository.findByStockItemIdAndSupplierId(7L, 3L)).thenReturn(Optional.of(tariff));

        // Se compró por unidades a 0,40: la tarifa por caja pasa a 24 x 0,40 = 9,60
        service.recordPurchasePrice(7L, 3L, "ud", new BigDecimal("0.40"), LocalDate.now(), "AL-1");

        verify(priceHistoryRepository).save(any(PurchasePriceHistory.class));
        assertThat(tariff.getUnitPrice()).isEqualByComparingTo("9.60");
        verify(supplierPriceRepository).save(tariff);
        // Otros módulos (escandallos) se enteran del cambio de coste sin que compras los conozca
        verify(events).publishEvent(new StockItemCostChanged(7L));
    }

    private static StockItemDTO water(List<ItemUnitConversionDTO> conversions) {
        return new StockItemDTO(null, "agua-50", "Agua 50 cl", 1L, null, "caja", "ud", "ud",
                conversions, null, 2L, null, null, false, null, null);
    }
}
