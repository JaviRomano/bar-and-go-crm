package com.barandgo.crm.masterdata.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UnitConversionRepository extends JpaRepository<UnitConversion, Long> {
}
