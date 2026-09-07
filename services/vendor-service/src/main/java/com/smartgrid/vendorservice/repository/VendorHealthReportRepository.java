package com.smartgrid.vendorservice.repository;

import com.smartgrid.vendorservice.domain.VendorHealthReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VendorHealthReportRepository extends JpaRepository<VendorHealthReport, UUID> {
    List<VendorHealthReport> findByVendorIdOrderByCreatedAtDesc(UUID vendorId);
}
