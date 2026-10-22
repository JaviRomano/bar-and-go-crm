package com.barandgo.crm.inventory.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {

    List<GoodsReceipt> findAllByOrderByIdDesc();

    @Query(value = "SELECT nextval('goods_receipt_number_seq')", nativeQuery = true)
    long nextNumberSequence();
}
