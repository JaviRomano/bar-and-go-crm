package com.barandgo.crm.costcontrol.internal;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.barandgo.crm.ordering.OrderEvents.OrderDelivered;
import com.barandgo.crm.ordering.OrderEvents.OrderDeliveryReverted;

/**
 * Descuento de stock por venta. Asíncrono, después del commit del pedido y en su propia transacción: si
 * falla (no hay almacén de ventas, la BD cae), el pedido sigue entregado y el evento queda pendiente en
 * el registro de Modulith hasta que se reprocesa.
 */
@Component
class OrderStockListener {

    private final CostControlServiceImpl costControl;

    OrderStockListener(CostControlServiceImpl costControl) {
        this.costControl = costControl;
    }

    @ApplicationModuleListener
    public void on(OrderDelivered event) {
        costControl.postDeliveredOrder(event);
    }

    @ApplicationModuleListener
    public void on(OrderDeliveryReverted event) {
        costControl.revertDeliveredOrder(event);
    }
}
