package com.barandgo.crm.inventory;

import java.util.List;

import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.inventory.ReplenishmentDTOs.CreateOrdersRequest;
import com.barandgo.crm.inventory.ReplenishmentDTOs.SupplierGroup;
import com.barandgo.crm.purchasing.PurchaseOrderDTO;

/**
 * Artículos bajo mínimo (contando lo ya pedido y pendiente de recibir) y su pedido sugerido.
 */
public interface ReplenishmentService {

    List<SupplierGroup> getSuggestions();

    /** Crea un pedido en borrador por proveedor con las líneas sugeridas; se revisa antes de enviarlo. */
    List<PurchaseOrderDTO> createDraftOrders(CreateOrdersRequest request, CurrentUser requester);
}
