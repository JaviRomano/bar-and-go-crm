package com.barandgo.crm.purchasing.internal;

import static com.barandgo.crm.purchasing.internal.PurchasingFixtures.CAJA;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.identity.UserRole;
import com.barandgo.crm.masterdata.SupplierService;
import com.barandgo.crm.masterdata.UnitOfMeasureService;
import com.barandgo.crm.masterdata.VatRateDTO;
import com.barandgo.crm.masterdata.VatRateService;
import com.barandgo.crm.purchasing.PurchaseOrderDTO;
import com.barandgo.crm.purchasing.PurchaseOrderLineRequestDTO;
import com.barandgo.crm.purchasing.PurchaseOrderRequestDTO;
import com.barandgo.crm.purchasing.PurchaseOrderStatus;
import com.barandgo.crm.shared.BadRequestException;
import com.barandgo.crm.shared.ConflictException;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceImplTest {

    private static final CurrentUser MANAGER = new CurrentUser(4L, "encargado@bar.com", UserRole.MANAGER);
    private static final long SUPPLIER = 3L;
    private static final long WATER = 7L;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private StockItemRepository stockItemRepository;

    @Mock
    private StockItemSupplierRepository supplierPriceRepository;

    @Mock
    private UnitOfMeasureService unitService;

    @Mock
    private SupplierService supplierService;

    @Mock
    private VatRateService vatRateService;

    private PurchaseOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        PurchasingFixtures.stubUnits(unitService);
        lenient().when(supplierService.getSupplierById(SUPPLIER)).thenReturn(PurchasingFixtures.supplier(SUPPLIER, true, 2));
        lenient().when(stockItemRepository.findById(WATER)).thenReturn(Optional.of(PurchasingFixtures.bottledWater(WATER)));
        lenient().when(vatRateService.getVatRate(2L))
                .thenReturn(new VatRateDTO(2L, "Reducido", new BigDecimal("10.00"), true));
        lenient().when(purchaseOrderRepository.nextNumberSequence()).thenReturn(7L);
        lenient().when(purchaseOrderRepository.save(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        service = new PurchaseOrderServiceImpl(purchaseOrderRepository, stockItemRepository, supplierPriceRepository,
                new UnitCatalog(unitService), supplierService, vatRateService);
    }

    @Test
    void unitAndPriceComeFromSupplierTariff() {
        stubTariff("caja", "12.00");

        PurchaseOrderDTO order = service.createPurchaseOrder(request(line(WATER, "2", null, null)), MANAGER);

        assertThat(order.number()).isEqualTo("PC-%d-00007".formatted(LocalDate.now().getYear()));
        assertThat(order.status()).isEqualTo(PurchaseOrderStatus.DRAFT);
        assertThat(order.createdBy()).isEqualTo(4L);
        assertThat(order.lines()).singleElement().satisfies(line -> {
            assertThat(line.unit()).isEqualTo("caja");
            assertThat(line.unitPrice()).isEqualByComparingTo("12.00");
            assertThat(line.vatRate()).isEqualByComparingTo("10.00");
            assertThat(line.amount()).isEqualByComparingTo("24.00");
        });
        assertThat(order.totalBase()).isEqualByComparingTo("24.00");
        assertThat(order.totalVat()).isEqualByComparingTo("2.40");
        assertThat(order.total()).isEqualByComparingTo("26.40");
    }

    @Test
    void tariffPriceIsConvertedWhenOrderingInAnotherUnit() {
        stubTariff("caja", "12.00");

        PurchaseOrderDTO order = service.createPurchaseOrder(request(line(WATER, "30", "ud", null)), MANAGER);

        // 12 € la caja de 24 -> 0,50 € la unidad
        assertThat(order.lines().get(0).unitPrice()).isEqualByComparingTo("0.50");
        assertThat(order.totalBase()).isEqualByComparingTo("15.00");
    }

    @Test
    void explicitPriceOverridesTariff() {
        stubTariff("caja", "12.00");

        PurchaseOrderDTO order = service.createPurchaseOrder(
                request(line(WATER, "1", null, new BigDecimal("11.00"))), MANAGER);

        assertThat(order.lines().get(0).unitPrice()).isEqualByComparingTo("11.00");
    }

    @Test
    void lineWithoutTariffNeedsExplicitPrice() {
        when(supplierPriceRepository.findByStockItemIdAndSupplierId(WATER, SUPPLIER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createPurchaseOrder(request(line(WATER, "1", null, null)), MANAGER))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("unitPrice");
        verify(purchaseOrderRepository, never()).save(any());
    }

    @Test
    void sameItemCannotBeInTwoLines() {
        stubTariff("caja", "12.00");

        PurchaseOrderRequestDTO duplicated = new PurchaseOrderRequestDTO(SUPPLIER, null, null,
                List.of(line(WATER, "1", null, null), line(WATER, "2", null, null)));

        assertThatThrownBy(() -> service.createPurchaseOrder(duplicated, MANAGER))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("más de una línea");
    }

    @Test
    void expectedDateDefaultsToSupplierLeadTime() {
        stubTariff("caja", "12.00");

        PurchaseOrderDTO order = service.createPurchaseOrder(request(line(WATER, "1", null, null)), MANAGER);

        assertThat(order.expectedDate()).isEqualTo(LocalDate.now().plusDays(2));
    }

    @Test
    void inactiveSupplierIsRejected() {
        when(supplierService.getSupplierById(SUPPLIER)).thenReturn(PurchasingFixtures.supplier(SUPPLIER, false, 2));

        assertThatThrownBy(() -> service.createPurchaseOrder(request(line(WATER, "1", null, null)), MANAGER))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("desactivado");
    }

    @Test
    void onlyDraftsCanBeUpdatedOrDeleted() {
        PurchaseOrder sent = new PurchaseOrder();
        PurchaseOrderLine line = new PurchaseOrderLine();
        line.setQuantity(BigDecimal.ONE);
        sent.addLine(line);
        sent.send();
        when(purchaseOrderRepository.findById(50L)).thenReturn(Optional.of(sent));

        assertThatThrownBy(() -> service.updatePurchaseOrder(50L, request(line(WATER, "1", null, null))))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.deletePurchaseOrder(50L))
                .isInstanceOf(ConflictException.class);
        verify(purchaseOrderRepository, never()).delete(any());
    }

    private void stubTariff(String unitCode, String price) {
        StockItemSupplier tariff = new StockItemSupplier(PurchasingFixtures.bottledWater(WATER), SUPPLIER);
        tariff.setPurchaseUnitId("caja".equals(unitCode) ? CAJA : PurchasingFixtures.UD);
        tariff.setUnitPrice(new BigDecimal(price));
        when(supplierPriceRepository.findByStockItemIdAndSupplierId(WATER, SUPPLIER)).thenReturn(Optional.of(tariff));
    }

    private static PurchaseOrderRequestDTO request(PurchaseOrderLineRequestDTO line) {
        return new PurchaseOrderRequestDTO(SUPPLIER, null, null, List.of(line));
    }

    private static PurchaseOrderLineRequestDTO line(Long itemId, String quantity, String unit, BigDecimal price) {
        return new PurchaseOrderLineRequestDTO(itemId, new BigDecimal(quantity), unit, price);
    }
}
