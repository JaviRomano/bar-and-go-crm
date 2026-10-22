package com.barandgo.crm.purchasing.internal;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findAllByOrderByIdDesc();

    // Secuencia propia: números correlativos sin depender del id ni de huecos por borrados
    @Query(value = "SELECT nextval('purchase_order_number_seq')", nativeQuery = true)
    long nextNumberSequence();
}
