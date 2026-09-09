package com.smartgrid.contractagent.web;

import com.smartgrid.contractagent.domain.NegotiationRun;
import com.smartgrid.contractagent.repository.NegotiationRunRepository;
import com.smartgrid.contractagent.service.ContractNegotiationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class NegotiationController {

    private final NegotiationRunRepository runRepository;
    private final ContractNegotiationService negotiationService;

    public NegotiationController(NegotiationRunRepository runRepository, ContractNegotiationService negotiationService) {
        this.runRepository = runRepository;
        this.negotiationService = negotiationService;
    }

    @GetMapping("/negotiations/{vendorId}")
    public List<NegotiationRun> getRuns(@PathVariable String vendorId) {
        return runRepository.findByVendorIdOrderByCreatedAtDesc(vendorId);
    }

    /** Drives the same real negotiation a live SLARiskReportGenerated/ContractExpiring event would — for manual dev triggering, per the module spec's "POST /agents/{agentId}/simulate" requirement. */
    @PostMapping("/negotiations/simulate")
    public NegotiationRun simulate(@Valid @RequestBody SimulateRequest request) {
        return negotiationService.negotiate(request.vendorId());
    }

    public record SimulateRequest(@NotBlank String vendorId) {
    }
}
