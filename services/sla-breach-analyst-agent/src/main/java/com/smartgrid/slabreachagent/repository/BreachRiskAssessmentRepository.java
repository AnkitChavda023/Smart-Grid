package com.smartgrid.slabreachagent.repository;

import com.smartgrid.slabreachagent.domain.BreachRiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BreachRiskAssessmentRepository extends JpaRepository<BreachRiskAssessment, UUID> {
    List<BreachRiskAssessment> findByVendorIdOrderByCreatedAtDesc(String vendorId);
}
