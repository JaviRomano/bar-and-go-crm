package com.barandgo.crm.inventory.internal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.barandgo.crm.masterdata.UnitConverter;
import com.barandgo.crm.masterdata.UnitOfMeasureService;
import com.barandgo.crm.purchasing.StockItemDTO;
import com.barandgo.crm.purchasing.StockItemService;
import com.barandgo.crm.purchasing.StockItemUnits;

/**
 * Artículos de compra vistos desde inventario: datos del artículo y su conversor de unidades
 * (reglas genéricas más las propias del artículo).
 */
@Component
class ItemCatalog {

    static final int QUANTITY_SCALE = 4;

    private final StockItemService stockItemService;
    private final UnitOfMeasureService unitService;

    ItemCatalog(StockItemService stockItemService, UnitOfMeasureService unitService) {
        this.stockItemService = stockItemService;
        this.unitService = unitService;
    }

    Item get(Long stockItemId) {
        StockItemDTO dto = stockItemService.getStockItem(stockItemId);
        return new Item(dto, converterFor(dto, unitService.genericConverter()));
    }

    /** Todos los artículos, activos o no, por id. */
    Map<Long, StockItemDTO> all() {
        return stockItemService.getStockItems(null, null).stream()
                .collect(Collectors.toMap(StockItemDTO::id, Function.identity()));
    }

    UnitConverter genericConverter() {
        return unitService.genericConverter();
    }

    static UnitConverter converterFor(StockItemDTO item, UnitConverter generic) {
        return StockItemUnits.converterFor(item, generic);
    }

    record Item(StockItemDTO dto, UnitConverter converter) {

        Long id() {
            return dto.id();
        }

        String stockUnit() {
            return dto.stockUnit();
        }

        /** Cantidad en unidad de inventario, con la escala de las columnas de stock. */
        BigDecimal toStockUnit(BigDecimal quantity, String unit) {
            BigDecimal converted = unit == null || unit.equals(dto.stockUnit())
                    ? quantity
                    : converter.convert(quantity, unit, dto.stockUnit());
            return converted.setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
        }
    }
}
