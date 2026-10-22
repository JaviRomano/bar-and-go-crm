package com.barandgo.crm.inventory;

import java.util.List;

/**
 * Almacenes. Sin borrado: un almacén con movimientos se desactiva.
 */
public interface WarehouseService {

    List<WarehouseDTO> getWarehouses(boolean includeInactive);

    WarehouseDTO createWarehouse(WarehouseDTO warehouseDTO);

    WarehouseDTO updateWarehouse(Long id, WarehouseDTO warehouseDTO);
}
