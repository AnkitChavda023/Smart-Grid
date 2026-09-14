package com.smartgrid.vendorservice.repository;

import com.smartgrid.vendorservice.domain.VendorScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VendorScoreRepository extends JpaRepository<VendorScore, UUID> {

    Optional<VendorScore> findByVendorId(UUID vendorId);
}
