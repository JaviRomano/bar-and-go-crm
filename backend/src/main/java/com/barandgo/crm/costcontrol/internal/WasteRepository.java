package com.barandgo.crm.costcontrol.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WasteRepository extends JpaRepository<Waste, Long>, JpaSpecificationExecutor<Waste> {
}
