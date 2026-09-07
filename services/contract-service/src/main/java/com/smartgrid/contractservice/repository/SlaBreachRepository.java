package com.smartgrid.contractservice.repository;

import com.smartgrid.contractservice.domain.SlaBreach;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SlaBreachRepository extends JpaRepository<SlaBreach, UUID> {

    List<SlaBreach> findByVendorId(String vendorId);

    long countByVendorId(String vendorId);
}
