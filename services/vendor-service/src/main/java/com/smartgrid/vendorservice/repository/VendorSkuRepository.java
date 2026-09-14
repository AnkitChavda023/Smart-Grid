package com.smartgrid.vendorservice.repository;

import com.smartgrid.vendorservice.domain.VendorSku;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VendorSkuRepository extends JpaRepository<VendorSku, UUID> {

    List<VendorSku> findBySkuIdAndVendor_SuspendedFalse(String skuId);

    List<VendorSku> findByVendor_Id(UUID vendorId);
}
