package com.barandgo.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barandgo.crm.entity.UnitConversion;

@Repository
public interface UnitConversionRepository extends JpaRepository<UnitConversion, Long> {
}
