package com.smartgrid.contractagent.repository;

import com.smartgrid.contractagent.domain.NegotiationRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NegotiationRunRepository extends JpaRepository<NegotiationRun, UUID> {
    List<NegotiationRun> findByVendorIdOrderByCreatedAtDesc(String vendorId);
}
