package com.barandgo.crm.inventory.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    List<Warehouse> findAllByOrderByNameAsc();

    boolean existsBySiteIdAndNameIgnoreCase(Long siteId, String name);

    boolean existsBySiteIdAndNameIgnoreCaseAndIdNot(Long siteId, String name, Long id);
}
