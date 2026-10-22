package com.barandgo.crm.recipes.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductionBatchRepository extends JpaRepository<ProductionBatch, Long> {
}
