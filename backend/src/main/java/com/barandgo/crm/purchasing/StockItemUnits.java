package com.barandgo.crm.purchasing;

import java.util.List;

import com.barandgo.crm.masterdata.UnitConverter;

/**
 * Conversor de unidades de un artículo: las reglas genéricas más las propias del artículo
 * (1 caja = 24 ud). Lo usan inventario y escandallos para pasar cantidades a unidad de inventario.
 */
public final class StockItemUnits {

    private StockItemUnits() {
    }

    public static UnitConverter converterFor(StockItemDTO item, UnitConverter generic) {
        if (item.conversions() == null || item.conversions().isEmpty()) {
            return generic;
        }
        List<UnitConverter.Rule> rules = item.conversions().stream()
                .map(c -> new UnitConverter.Rule(c.fromUnit(), c.toUnit(), c.factor()))
                .toList();
        return generic.withItemRules(rules);
    }
}
