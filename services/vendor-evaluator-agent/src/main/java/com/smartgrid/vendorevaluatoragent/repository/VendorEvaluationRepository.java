package com.smartgrid.vendorevaluatoragent.repository;

import com.smartgrid.vendorevaluatoragent.domain.VendorEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VendorEvaluationRepository extends JpaRepository<VendorEvaluation, UUID> {
    List<VendorEvaluation> findByVendorIdOrderByCreatedAtDesc(String vendorId);
}
