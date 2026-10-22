package com.barandgo.crm.masterdata.internal;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findAllByOrderByLegalNameAsc();

    List<Supplier> findByActiveOrderByLegalNameAsc(Boolean active);

    boolean existsByTaxId(String taxId);

    boolean existsByTaxIdAndIdNot(String taxId, Long id);
}
