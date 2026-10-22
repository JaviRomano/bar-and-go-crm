package com.barandgo.crm.masterdata.internal;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VatRateRepository extends JpaRepository<VatRate, Long> {

    List<VatRate> findAllByOrderByRateDesc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
