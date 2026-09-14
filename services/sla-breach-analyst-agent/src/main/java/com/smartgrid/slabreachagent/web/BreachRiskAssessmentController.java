package com.smartgrid.slabreachagent.web;

import com.smartgrid.slabreachagent.domain.BreachRiskAssessment;
import com.smartgrid.slabreachagent.repository.BreachRiskAssessmentRepository;
import com.smartgrid.slabreachagent.service.BreachRiskAssessmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BreachRiskAssessmentController {

    private final BreachRiskAssessmentRepository assessmentRepository;
    private final BreachRiskAssessmentService assessmentService;

    public BreachRiskAssessmentController(BreachRiskAssessmentRepository assessmentRepository, BreachRiskAssessmentService assessmentService) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentService = assessmentService;
    }

    @GetMapping("/breach-assessments/{vendorId}")
    public List<BreachRiskAssessment> getAssessments(@PathVariable String vendorId) {
        return assessmentRepository.findByVendorIdOrderByCreatedAtDesc(vendorId);
    }

    /** Drives the same real assessment a live SLABreached event would — for manual dev triggering, per the module spec's "POST /agents/{agentId}/simulate" requirement. */
    @PostMapping("/breach-assessments/simulate")
    public BreachRiskAssessment simulate(@Valid @RequestBody SimulateRequest request) {
        return assessmentService.assess(request.vendorId());
    }

    public record SimulateRequest(@NotBlank String vendorId) {
    }
}
