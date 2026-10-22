package com.barandgo.crm.purchasing;

import java.util.List;

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
}
