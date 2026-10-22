package com.barandgo.crm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barandgo.crm.entity.UnitOfMeasure;

@Repository
public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, Long> {

    List<UnitOfMeasure> findAllByOrderByDimensionAscCodeAsc();

    Optional<UnitOfMeasure> findByCode(String code);

    boolean existsByCode(String code);
}
