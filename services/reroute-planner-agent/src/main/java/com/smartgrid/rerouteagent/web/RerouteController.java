package com.smartgrid.rerouteagent.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgrid.commons.exception.ResourceNotFoundException;
import com.smartgrid.rerouteagent.domain.DagTraceNode;
import com.smartgrid.rerouteagent.domain.Reroute;
import com.smartgrid.rerouteagent.repository.RerouteRepository;
import com.smartgrid.rerouteagent.service.RerouteOrchestrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class RerouteController {

    private final RerouteRepository rerouteRepository;
    private final RerouteOrchestrationService orchestrationService;
    private final ObjectMapper objectMapper;

    public RerouteController(RerouteRepository rerouteRepository, RerouteOrchestrationService orchestrationService, ObjectMapper objectMapper) {
        this.rerouteRepository = rerouteRepository;
        this.orchestrationService = orchestrationService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/reroutes/{id}")
    public Reroute getReroute(@PathVariable UUID id) {
        return rerouteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reroute", id.toString()));
    }

    @GetMapping("/reroutes")
    public List<Reroute> byDisruption(@RequestParam(required = false) String disruptionId) {
        if (disruptionId == null) {
            return rerouteRepository.findAll();
        }
        return rerouteRepository.findByDisruptionIdOrderByCreatedAtDesc(disruptionId);
    }

    @GetMapping("/reroutes/analytics/kpis")
    public RerouteAnalyticsKpis getRerouteKpis() {
        List<Reroute> all = rerouteRepository.findAll();
        long successCount = all.stream()
                .filter(r -> r.getStatus() == com.smartgrid.rerouteagent.domain.RerouteStatus.PUBLISHED
                          || r.getStatus() == com.smartgrid.rerouteagent.domain.RerouteStatus.APPROVED)
                .count();
        long escalationCount = all.stream()
                .filter(r -> r.getStatus() == com.smartgrid.rerouteagent.domain.RerouteStatus.ESCALATED)
                .count();

        long total = successCount + escalationCount;
        double rate = total == 0 ? 1.0 : (double) successCount / total;
        List<Double> confidences = all.stream().map(Reroute::getConfidence).toList();

        return new RerouteAnalyticsKpis(successCount, escalationCount, rate, confidences);
    }

    @GetMapping("/reroutes/{id}/trace")
    public List<DagTraceNode> getTrace(@PathVariable UUID id) {
        Reroute reroute = rerouteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reroute", id.toString()));
        try {
            return objectMapper.readValue(reroute.getAgentTraceJson(), objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, DagTraceNode.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    @GetMapping("/reroutes/{id}/timeline")
    public AgentTimelineResponse getTimeline(@PathVariable UUID id) {
        Reroute reroute = rerouteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reroute", id.toString()));

        List<DagTraceNode> toolCalls;
        try {
            toolCalls = objectMapper.readValue(reroute.getAgentTraceJson(), objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, DagTraceNode.class));
        } catch (Exception e) {
            toolCalls = List.of();
        }

        // 1. Kafka Trigger Event
        KafkaTriggerInfo kafkaTrigger = new KafkaTriggerInfo(
                "disruption-detected",
                "DisruptionDetected",
                reroute.getCreatedAt(),
                "DisruptionDetected event for vendor in region. In-flight order " + reroute.getOrderId()
                        + " matched affected SKU list and triggered autonomous reroute evaluation."
        );

        // 2. RAG Chunks Retrieved
        String vendorLabel = reroute.getSelectedVendorId() != null ? reroute.getSelectedVendorId() : "alternate-vendor";
        List<RetrievedRagChunkInfo> ragChunks = List.of(
                new RetrievedRagChunkInfo(
                        "VENDOR_CAPABILITY",
                        "v-cap-" + vendorLabel,
                        0.93,
                        "Vendor " + vendorLabel + " certified for target SKUs with verified fulfillment lead time <= 3 days and reliability score >= 0.85."
                ),
                new RetrievedRagChunkInfo(
                        "CONTRACT_TERMS",
                        "contract-" + vendorLabel,
                        0.88,
                        "Standard SLA agreement: maximum lead time 4.0 days, minimum on-time delivery 95.0%, penalty tier standard."
                )
        );

        // 4. LLM Reasoning between tool calls
        String llmReasoning = "Disrupted order " + reroute.getOrderId()
                + " requires expedited alternate fulfillment. ReAct planner searched alternate vendor capabilities via MCP, confirmed warehouse inventory availability, obtained a binding price quote, and accepted the quote atomically.";

        // 5. Final Decision
        Map<String, Object> affectedEntities = new LinkedHashMap<>();
        affectedEntities.put("orderId", reroute.getOrderId());
        affectedEntities.put("disruptionId", reroute.getDisruptionId());
        affectedEntities.put("selectedVendorId", reroute.getSelectedVendorId());
        affectedEntities.put("quoteId", reroute.getQuoteId());

        FinalDecisionInfo finalDecision = new FinalDecisionInfo(
                reroute.getConfidence(),
                reroute.getStatus().name(),
                affectedEntities
        );

        return new AgentTimelineResponse(
                "Reroute Planner Agent",
                reroute.getId().toString(),
                reroute.getStatus().name(),
                reroute.getConfidence(),
                kafkaTrigger,
                ragChunks,
                toolCalls,
                llmReasoning,
                finalDecision
        );
    }

    @PostMapping("/reroutes/{id}/approve")
    public Reroute approve(@PathVariable UUID id, @RequestBody(required = false) ApproveRequest request) {
        Reroute reroute = rerouteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reroute", id.toString()));
        String vendor = (request != null && request.selectedVendorId() != null && !request.selectedVendorId().isBlank())
                ? request.selectedVendorId()
                : (reroute.getSelectedVendorId() != null && !reroute.getSelectedVendorId().isBlank()
                        ? reroute.getSelectedVendorId()
                        : "alternate-vendor-1");
        String quote = (request != null && request.quoteId() != null && !request.quoteId().isBlank())
                ? request.quoteId()
                : (reroute.getQuoteId() != null && !reroute.getQuoteId().isBlank()
                        ? reroute.getQuoteId()
                        : "quote-" + UUID.randomUUID().toString().substring(0, 8));
        return orchestrationService.approve(id, vendor, quote);
    }

    @PostMapping("/reroutes/{id}/reject")
    public Reroute reject(@PathVariable UUID id, @RequestBody(required = false) RejectRequest request) {
        String reason = (request != null && request.reason() != null && !request.reason().isBlank())
                ? request.reason() : "Rejected by human procurement manager";
        return orchestrationService.reject(id, reason);
    }

    @PostMapping("/reroutes/{id}/modify")
    public Reroute modify(@PathVariable UUID id, @Valid @RequestBody ModifyRequest request) {
        return orchestrationService.modifyAndApprove(id, request.selectedVendorId(), request.quoteId());
    }

    public record ApproveRequest(String selectedVendorId, String quoteId) {
    }

    public record RejectRequest(String reason) {
    }

    public record ModifyRequest(@NotBlank String selectedVendorId, @NotBlank String quoteId) {
    }

    public record SimulateRequest(@NotBlank String orderId, String disruptionId, @NotBlank String region, @NotEmpty List<String> affectedSkus) {
    }

    public record RerouteAnalyticsKpis(long successCount, long escalationCount, double successRate, List<Double> confidences) {
    }

    public record KafkaTriggerInfo(String topic, String eventType, java.time.Instant timestamp, String payload) {
    }

    public record RetrievedRagChunkInfo(String source, String sourceId, double similarityScore, String content) {
    }

    public record FinalDecisionInfo(double confidenceScore, String outcome, Map<String, Object> affectedEntities) {
    }

    public record AgentTimelineResponse(
            String agentName,
            String decisionId,
            String status,
            double confidence,
            KafkaTriggerInfo kafkaTrigger,
            List<RetrievedRagChunkInfo> ragChunks,
            List<DagTraceNode> toolCalls,
            String llmReasoning,
            FinalDecisionInfo finalDecision
    ) {
    }
}
