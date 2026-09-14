package com.smartgrid.vendorevaluatoragent.web;

import com.smartgrid.vendorevaluatoragent.domain.VendorEvaluation;
import com.smartgrid.vendorevaluatoragent.repository.VendorEvaluationRepository;
import com.smartgrid.vendorevaluatoragent.service.VendorEvaluationService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class VendorEvaluationController {

    private final VendorEvaluationRepository evaluationRepository;
    private final VendorEvaluationService evaluationService;

    public VendorEvaluationController(VendorEvaluationRepository evaluationRepository, VendorEvaluationService evaluationService) {
        this.evaluationRepository = evaluationRepository;
        this.evaluationService = evaluationService;
    }

    @GetMapping("/vendor-evaluations/{vendorId}")
    public List<VendorEvaluation> getEvaluations(@PathVariable String vendorId) {
        return evaluationRepository.findByVendorIdOrderByCreatedAtDesc(vendorId);
    }

    /** Drives the same real evaluation a live RerouteDecision event would — for manual dev triggering, per the module spec's "POST /agents/{agentId}/simulate" requirement. */
    @PostMapping("/vendor-evaluations/simulate")
    public VendorEvaluation simulate(@Valid @RequestBody SimulateRequest request) {
        return evaluationService.evaluate(request.vendorId());
    }

    public record SimulateRequest(@NotBlank String vendorId) {
    }
}
