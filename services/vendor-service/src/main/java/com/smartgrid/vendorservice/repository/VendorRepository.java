package com.smartgrid.vendorservice.repository;

import com.smartgrid.vendorservice.domain.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {
}
