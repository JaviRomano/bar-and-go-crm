package com.barandgo.crm.inventory.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {

    Optional<Site> findFirstByOrderByIdAsc();
}
