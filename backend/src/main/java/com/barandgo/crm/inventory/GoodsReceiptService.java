package com.barandgo.crm.inventory;

import java.util.List;

import com.barandgo.crm.identity.CurrentUser;
import com.barandgo.crm.inventory.GoodsReceiptDTOs.GoodsReceiptDTO;
import com.barandgo.crm.inventory.GoodsReceiptDTOs.GoodsReceiptRequestDTO;

/**
 * Recepción de mercancía. Registrar un albarán genera las entradas de stock, actualiza el pedido y
 * el precio de compra. Un albarán registrado no se modifica: los errores se corrigen con un ajuste.
 */
public interface GoodsReceiptService {

    GoodsReceiptDTO registerReceipt(GoodsReceiptRequestDTO request, CurrentUser requester);

    GoodsReceiptDTO getReceipt(Long id);

    /** Filtro opcional por proveedor; más recientes primero. */
    List<GoodsReceiptDTO> getReceipts(Long supplierId);
}
