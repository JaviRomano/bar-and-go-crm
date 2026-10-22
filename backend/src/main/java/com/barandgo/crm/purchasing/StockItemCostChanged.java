package com.barandgo.crm.purchasing;

/**
 * Ha podido cambiar el coste de reposición de un artículo: tarifa nueva o modificada, compra
 * registrada, proveedor habitual o unidades distintas. Lo escuchan otros módulos (escandallos) para
 * recalcular; compras no sabe quién.
 */
public record StockItemCostChanged(Long stockItemId) {
}
