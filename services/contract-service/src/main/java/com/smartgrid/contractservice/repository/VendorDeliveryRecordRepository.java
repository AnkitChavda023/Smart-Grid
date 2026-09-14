package com.smartgrid.contractservice.repository;

import com.smartgrid.contractservice.domain.VendorDeliveryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VendorDeliveryRecordRepository extends JpaRepository<VendorDeliveryRecord, String> {

    List<VendorDeliveryRecord> findByDeliveredFalse();

    List<VendorDeliveryRecord> findByVendorId(String vendorId);
}
