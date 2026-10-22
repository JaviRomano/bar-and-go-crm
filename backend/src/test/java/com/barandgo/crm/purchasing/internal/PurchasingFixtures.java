package com.barandgo.crm.purchasing.internal;

import static org.mockito.Mockito.lenient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.barandgo.crm.masterdata.SupplierDTO;
import com.barandgo.crm.masterdata.UnitConverter;
import com.barandgo.crm.masterdata.UnitDimension;
import com.barandgo.crm.masterdata.UnitOfMeasureDTO;
import com.barandgo.crm.masterdata.UnitOfMeasureService;

/**
 * Datos de prueba compartidos por los tests de servicios de compras.
 */
final class PurchasingFixtures {

    static final long KG = 1L;
    static final long G = 2L;
    static final long L = 3L;
    static final long ML = 5L;
    static final long UD = 6L;
    static final long CAJA = 8L;

    private PurchasingFixtures() {
    }

    /** Unidades de la migración V6 con sus conversiones genéricas. */
    static void stubUnits(UnitOfMeasureService unitService) {
        lenient().when(unitService.getUnits()).thenReturn(List.of(
                new UnitOfMeasureDTO(KG, "kg", "Kilogramo", UnitDimension.MASS),
                new UnitOfMeasureDTO(G, "g", "Gramo", UnitDimension.MASS),
                new UnitOfMeasureDTO(L, "l", "Litro", UnitDimension.VOLUME),
                new UnitOfMeasureDTO(ML, "ml", "Mililitro", UnitDimension.VOLUME),
                new UnitOfMeasureDTO(UD, "ud", "Unidad", UnitDimension.COUNT),
                new UnitOfMeasureDTO(CAJA, "caja", "Caja", UnitDimension.COUNT)));
        lenient().when(unitService.genericConverter()).thenReturn(new UnitConverter(List.of(
                new UnitConverter.Rule("kg", "g", new BigDecimal("1000")),
                new UnitConverter.Rule("l", "ml", new BigDecimal("1000")))));
    }

    static SupplierDTO supplier(Long id, boolean active, Integer leadTimeDays) {
        return new SupplierDTO(id, "B12345674", "Proveedor " + id, null, null, null, null, null,
                leadTimeDays, null, active, Instant.now());
    }

    /** Agua en botella: se compra por caja de 24 y se cuenta por unidad. */
    static StockItem bottledWater(Long id) {
        StockItem item = new StockItem();
        item.setId(id);
        item.setCode("AGUA-50");
        item.setName("Agua 50 cl");
        item.setItemCategoryId(1L);
        item.setPurchaseUnitId(CAJA);
        item.setStockUnitId(UD);
        item.setUsageUnitId(UD);
        item.setVatRateId(2L);
        item.getConversions().add(new ItemUnitConversion(CAJA, UD, new BigDecimal("24")));
        return item;
    }
}
