package com.barandgo.crm.purchasing;

import java.util.List;

/**
 * Artículos de compra y sus precios por proveedor. Sin borrado: un artículo en desuso se desactiva.
 */
public interface StockItemService {

    /** Filtros opcionales: null no filtra. */
    List<StockItemDTO> getStockItems(Boolean active, Long itemCategoryId);

    StockItemDTO getStockItem(Long id);

    StockItemDTO createStockItem(StockItemDTO stockItemDTO);

    StockItemDTO updateStockItem(Long id, StockItemDTO stockItemDTO);

    List<StockItemSupplierDTO> getSuppliers(Long stockItemId);

    /** Crea o sustituye el precio del artículo en ese proveedor. */
    StockItemSupplierDTO setSupplierPrice(Long stockItemId, Long supplierId, StockItemSupplierDTO priceDTO);

    void removeSupplier(Long stockItemId, Long supplierId);
}
