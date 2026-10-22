package com.barandgo.crm.masterdata;

import java.util.List;


public interface SupplierService {

    /** Todos si active es null; si no, solo los activos o los inactivos. */
    List<SupplierDTO> getSuppliers(Boolean active);

    SupplierDTO getSupplierById(Long id);

    SupplierDTO createSupplier(SupplierDTO supplierDTO);

    SupplierDTO updateSupplier(Long id, SupplierDTO supplierDTO);
}
