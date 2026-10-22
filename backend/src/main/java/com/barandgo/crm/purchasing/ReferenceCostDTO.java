package com.barandgo.crm.purchasing;

import java.math.BigDecimal;

/**
 * Coste de reposición de un artículo: la tarifa del proveedor habitual o, si no la hay, la más barata
 * por unidad de inventario (solo proveedores activos). Tras cada recepción la tarifa es el último
 * precio pagado.
 *
 * @param unitPrice        precio de la tarifa, en su unidad
 * @param costPerStockUnit lo mismo por unidad de inventario del artículo
 */
public record ReferenceCostDTO(Long supplierId, String unit, BigDecimal unitPrice, BigDecimal costPerStockUnit) {
}
