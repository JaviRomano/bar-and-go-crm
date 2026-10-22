package com.barandgo.crm.inventory;

import java.util.List;
import java.util.Optional;

/**
 * Almacenes. Sin borrado: un almacén con movimientos se desactiva.
 */
public interface WarehouseService {

    List<WarehouseDTO> getWarehouses(boolean includeInactive);

    WarehouseDTO createWarehouse(WarehouseDTO warehouseDTO);

    WarehouseDTO updateWarehouse(Long id, WarehouseDTO warehouseDTO);

    /**
     * Almacén del que salen las ventas (simplificación: uno por local; la bebida de barra y la comida
     * de cocina salen del mismo). Vacío si no hay ninguno marcado.
     */
    Optional<WarehouseDTO> getSalesWarehouse();
}
