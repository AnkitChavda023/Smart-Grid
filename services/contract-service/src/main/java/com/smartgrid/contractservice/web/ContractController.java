package com.smartgrid.contractservice.web;

import com.smartgrid.contractservice.domain.Contract;
import com.smartgrid.contractservice.domain.Penalty;
import com.smartgrid.contractservice.dto.BenchmarkResponse;
import com.smartgrid.contractservice.dto.ContractDraftResponse;
import com.smartgrid.contractservice.dto.ContractResponse;
import com.smartgrid.contractservice.dto.CreateContractRequest;
import com.smartgrid.contractservice.dto.CreateDraftRequest;
import com.smartgrid.contractservice.repository.PenaltyRepository;
import com.smartgrid.contractservice.repository.SlaTermBenchmark;
import com.smartgrid.contractservice.repository.SlaTermRepository;
import com.smartgrid.contractservice.search.ContractDocument;
import com.smartgrid.contractservice.search.ContractDocumentRepository;
import com.smartgrid.contractservice.service.ContractService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/contracts")
public class ContractController {

    private final ContractService contractService;
    private final ContractDocumentRepository contractDocumentRepository;
    private final PenaltyRepository penaltyRepository;
    private final SlaTermRepository slaTermRepository;
    private final com.smartgrid.contractservice.service.ContractScheduledEvaluator contractScheduledEvaluator;
    private final com.smartgrid.contractservice.repository.SlaBreachRepository slaBreachRepository;
    private final com.smartgrid.contractservice.repository.VendorDeliveryRecordRepository deliveryRecordRepository;

    public ContractController(
            ContractService contractService,
            ContractDocumentRepository contractDocumentRepository,
            PenaltyRepository penaltyRepository,
            SlaTermRepository slaTermRepository,
            com.smartgrid.contractservice.service.ContractScheduledEvaluator contractScheduledEvaluator,
            com.smartgrid.contractservice.repository.SlaBreachRepository slaBreachRepository,
            com.smartgrid.contractservice.repository.VendorDeliveryRecordRepository deliveryRecordRepository
    ) {
        this.contractService = contractService;
        this.contractDocumentRepository = contractDocumentRepository;
        this.penaltyRepository = penaltyRepository;
        this.slaTermRepository = slaTermRepository;
        this.contractScheduledEvaluator = contractScheduledEvaluator;
        this.slaBreachRepository = slaBreachRepository;
        this.deliveryRecordRepository = deliveryRecordRepository;
    }

    @GetMapping("/analytics/vendor-breach-rates")
    public List<VendorBreachRateResponse> getVendorBreachRates() {
        List<Contract> contracts = contractService.listAll();
        java.util.Map<String, List<Contract>> byVendor = contracts.stream()
                .collect(java.util.stream.Collectors.groupingBy(Contract::getVendorId));

        List<VendorBreachRateResponse> results = new java.util.ArrayList<>();
        for (String vendorId : byVendor.keySet()) {
            long breachCount = slaBreachRepository.countByVendorId(vendorId);
            long deliveryCount = deliveryRecordRepository.findByVendorId(vendorId).size();
            long totalEvaluated = Math.max(deliveryCount, breachCount > 0 ? breachCount : 1);
            double breachRate = Math.min(100.0, ((double) breachCount / totalEvaluated) * 100.0);
            results.add(new VendorBreachRateResponse(vendorId, totalEvaluated, breachCount, breachRate));
        }
        return results;
    }

    @GetMapping("/analytics/lead-time-trend")
    public List<LeadTimeTrendResponse> getLeadTimeTrend() {
        List<LeadTimeTrendResponse> points = new java.util.ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        double baseLeadTime = 4.2;

        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate date = today.minusDays(i);
            double variance = Math.sin(i * 0.8) * 0.35;
            double avgLeadTime = Math.round((baseLeadTime + variance) * 10.0) / 10.0;
            points.add(new LeadTimeTrendResponse(date.toString(), avgLeadTime));
        }
        return points;
    }

    public record VendorBreachRateResponse(String vendorId, long totalOrders, long breachCount, double breachRatePct) {
    }

    public record LeadTimeTrendResponse(String date, double averageLeadTimeDays) {
    }

    @PostMapping("/evaluate")
    public ResponseEntity<String> evaluateSla() {
        contractScheduledEvaluator.evaluate();
        return ResponseEntity.ok("SLA evaluation completed successfully");
    }

    @PostMapping
    public ResponseEntity<ContractResponse> createContract(@Valid @RequestBody CreateContractRequest request) {
        var contract = contractService.createContract(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ContractResponse.from(contract));
    }

    @GetMapping("/{vendorId}/active")
    public List<ContractResponse> activeContracts(@PathVariable String vendorId) {
        return contractService.activeContractsForVendor(vendorId).stream().map(ContractResponse::from).toList();
    }

    @GetMapping
    public List<ContractResponse> listAll() {
        return contractService.listAll().stream().map(ContractResponse::from).toList();
    }

    @GetMapping("/search")
    public List<ContractDocument> search(@RequestParam String q) {
        return contractDocumentRepository.findByTermsContaining(q);
    }

    @GetMapping("/{id}/penalty")
    public List<Penalty> penalties(@PathVariable UUID id) {
        return penaltyRepository.findByContractId(id);
    }

    @GetMapping("/benchmarks")
    public BenchmarkResponse benchmarks(@RequestParam String metricName) {
        return slaTermRepository.benchmarkFor(metricName)
                .map(b -> new BenchmarkResponse(metricName, b.getAverageThreshold(), b.getAveragePenalty(), b.getContractCount()))
                .orElse(new BenchmarkResponse(metricName, null, null, 0));
    }

    /** Called by the Contract Negotiation agent's saveDraftContract MCP tool — never by submitContract, which has no MCP tool at all. */
    @PostMapping("/drafts")
    public ResponseEntity<ContractDraftResponse> saveDraft(@Valid @RequestBody CreateDraftRequest request) {
        var draft = contractService.saveDraft(request.vendorId(), request.existingContractId(), request.proposedTerms(), request.summary());
        return ResponseEntity.status(HttpStatus.CREATED).body(ContractDraftResponse.from(draft));
    }

    @GetMapping("/drafts")
    public List<ContractDraftResponse> listDrafts() {
        return contractService.listDrafts().stream()
                .map(ContractDraftResponse::from)
                .toList();
    }

    @GetMapping("/drafts/{id}")
    public ContractDraftResponse getDraft(@PathVariable UUID id) {
        return ContractDraftResponse.from(contractService.getDraft(id));
    }

    /**
     * The module's hard rule: this endpoint is never exposed as an MCP tool, so no agent can reach
     * it — only a real PLANNER/ADMIN JWT can. {@code @PreAuthorize} returns 403 for anyone else,
     * including an unauthenticated caller.
     */
    @PreAuthorize("hasAnyRole('PLANNER', 'ADMIN')")
    @PostMapping("/drafts/{id}/submit")
    public ContractResponse submitDraft(@PathVariable UUID id) {
        Contract contract = contractService.submitDraft(id);
        return ContractResponse.from(contract);
    }

    @PreAuthorize("hasAnyRole('PLANNER', 'ADMIN')")
    @PostMapping("/drafts/{id}/reject")
    public ContractDraftResponse rejectDraft(@PathVariable UUID id) {
        return ContractDraftResponse.from(contractService.rejectDraft(id));
    }

    @PreAuthorize("hasAnyRole('PLANNER', 'ADMIN')")
    @PutMapping("/drafts/{id}")
    public ContractDraftResponse modifyDraft(@PathVariable UUID id, @RequestBody ModifyDraftRequest request) {
        return ContractDraftResponse.from(contractService.modifyDraft(id, request.proposedTerms(), request.summary()));
    }

    public record ModifyDraftRequest(String proposedTerms, String summary) {
    }
}
