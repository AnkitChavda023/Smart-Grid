package com.smartgrid.vendorservice.repository;

import com.smartgrid.vendorservice.domain.SkuDemandCounter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkuDemandCounterRepository extends JpaRepository<SkuDemandCounter, String> {
}
