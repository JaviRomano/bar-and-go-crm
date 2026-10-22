package com.barandgo.crm.purchasing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.barandgo.crm.identity.CurrentUser;

/**
 * Pedidos a proveedor. Solo un borrador se modifica o se borra; después se cancela.
 */
public interface PurchaseOrderService {

    /** Filtros opcionales: null no filtra. Más recientes primero. */
    List<PurchaseOrderDTO> getPurchaseOrders(PurchaseOrderStatus status, Long supplierId);

    PurchaseOrderDTO getPurchaseOrder(Long id);

    PurchaseOrderDTO createPurchaseOrder(PurchaseOrderRequestDTO request, CurrentUser requester);

    /** Sustituye proveedor, fecha, notas y líneas de un borrador. */
    PurchaseOrderDTO updatePurchaseOrder(Long id, PurchaseOrderRequestDTO request);

    PurchaseOrderDTO sendPurchaseOrder(Long id);

    PurchaseOrderDTO cancelPurchaseOrder(Long id);

    void deletePurchaseOrder(Long id);

    /** Da por recibido un pedido enviado aunque falte mercancía. */
    PurchaseOrderDTO closePurchaseOrder(Long id);

    /**
     * Suma lo recibido en un albarán a las líneas del pedido y actualiza su estado.
     *
     * @param receivedByLineId cantidad recibida por id de línea, en la unidad de cada línea
     */
    void registerReceipt(Long id, Map<Long, BigDecimal> receivedByLineId);
}
