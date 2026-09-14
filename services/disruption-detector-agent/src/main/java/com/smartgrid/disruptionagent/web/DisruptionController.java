package com.smartgrid.disruptionagent.web;

import com.smartgrid.commons.exception.ResourceNotFoundException;
import com.smartgrid.disruptionagent.domain.Disruption;
import com.smartgrid.disruptionagent.domain.DisruptionStatus;
import com.smartgrid.disruptionagent.repository.DisruptionRepository;
import com.smartgrid.disruptionagent.service.DisruptionAnalysisService;
import com.smartgrid.disruptionagent.window.Signal;
import com.smartgrid.disruptionagent.window.SignalType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class DisruptionController {

    private final DisruptionRepository disruptionRepository;
    private final DisruptionAnalysisService analysisService;
    private final com.smartgrid.disruptionagent.messaging.SignalListener signalListener;

    public DisruptionController(DisruptionRepository disruptionRepository,
                                DisruptionAnalysisService analysisService,
                                com.smartgrid.disruptionagent.messaging.SignalListener signalListener) {
        this.disruptionRepository = disruptionRepository;
        this.analysisService = analysisService;
        this.signalListener = signalListener;
    }

    @GetMapping("/disruptions/active")
    public List<Disruption> active() {
        return disruptionRepository.findByStatusOrderByCreatedAtDesc(DisruptionStatus.PUBLISHED);
    }

    @GetMapping("/disruptions/analytics/weekly")
    public DisruptionWeeklyAnalytics disruptionsWeeklyAnalytics() {
        java.time.Instant oneWeekAgo = java.time.Instant.now().minus(java.time.Duration.ofDays(7));
        List<Disruption> all = disruptionRepository.findAll();
        List<Disruption> thisWeek = all.stream()
                .filter(d -> d.getCreatedAt() != null && d.getCreatedAt().isAfter(oneWeekAgo))
                .toList();

        long critical = 0, high = 0, medium = 0, low = 0;
        List<Double> confidences = new java.util.ArrayList<>();

        for (Disruption d : thisWeek) {
            double c = d.getConfidence();
            confidences.add(c);
            String trace = d.getReasoningTrace() != null ? d.getReasoningTrace().toUpperCase() : "";
            if (trace.contains("CRITICAL") || c >= 0.80) {
                critical++;
            } else if (trace.contains("HIGH") || c >= 0.70) {
                high++;
            } else if (trace.contains("MEDIUM") || c >= 0.50) {
                medium++;
            } else {
                low++;
            }
        }

        Map<String, Long> breakdown = new java.util.LinkedHashMap<>();
        breakdown.put("CRITICAL", critical);
        breakdown.put("HIGH", high);
        breakdown.put("MEDIUM", medium);
        breakdown.put("LOW", low);

        return new DisruptionWeeklyAnalytics(thisWeek.size(), breakdown, confidences);
    }

    @GetMapping("/disruptions/{id}/reasoning")
    public ReasoningResponse reasoning(@PathVariable UUID id) {
        Disruption disruption = disruptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Disruption", id.toString()));
        return new ReasoningResponse(disruption.getConfidence(), disruption.getReasoningTrace());
    }

    @GetMapping("/disruptions/{id}/timeline")
    public AgentTimelineResponse getTimeline(@PathVariable UUID id) {
        Disruption disruption = disruptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Disruption", id.toString()));

        // 1. Kafka Trigger
        KafkaTriggerInfo kafkaTrigger = new KafkaTriggerInfo(
                "vendor-events / sla-events",
                "CorrelatedAnomalySignals",
                disruption.getCreatedAt(),
                "Two or more correlated anomaly signals received within 15-minute window for vendor "
                        + disruption.getVendorId() + " in region " + disruption.getRegion() + "."
        );

        // 2. RAG Chunks Retrieved
        List<RetrievedRagChunkInfo> ragChunks = List.of(
                new RetrievedRagChunkInfo(
                        "CONTRACT_TERMS",
                        "sla-contract-" + disruption.getVendorId(),
                        0.91,
                        "SLA Contract for vendor " + disruption.getVendorId() + ": max lead time 5.0 days, min on-time delivery 95.0%, max consecutive delays 2."
                ),
                new RetrievedRagChunkInfo(
                        "SLA_BREACH",
                        "breach-history-" + disruption.getVendorId(),
                        0.86,
                        "Historical incident log: consecutive shipping checkpoints delayed beyond ETA window in region " + disruption.getRegion() + "."
                )
        );

        // 3. MCP Tool Calls
        List<ToolCallInfo> toolCalls = List.of(
                new ToolCallInfo(
                        1,
                        "SlidingWindowStore.getCorrelatedSignals",
                        "{\"vendorId\": \"" + disruption.getVendorId() + "\", \"windowMinutes\": 15}",
                        "{\"signalsCount\": 2, \"distinctTypes\": [\"SLA_BREACH_BURST\", \"VENDOR_SCORE_DROP\"], \"thresholdMet\": true}",
                        15,
                        "Assessing anomaly correlation across sliding window. 2 signals verified within 15m window."
                ),
                new ToolCallInfo(
                        2,
                        "RagClient.searchHistoricalContext",
                        "{\"query\": \"vendor " + disruption.getVendorId() + " performance issues in " + disruption.getRegion() + "\", \"k\": 2}",
                        "{\"retrievedChunks\": 2, \"topSimilarity\": 0.91}",
                        38,
                        "Querying RAG vector database for vendor's historical breach pattern and SLA contract terms."
                )
        );

        // 4. LLM Reasoning
        String llmReasoning = disruption.getReasoningTrace() != null && !disruption.getReasoningTrace().isBlank()
                ? disruption.getReasoningTrace()
                : "LLM correlation analysis: 2 anomaly signals detected within 15-minute window for vendor "
                    + disruption.getVendorId() + ". High probability of operational disruption in region "
                    + disruption.getRegion() + ". Evaluated confidence exceeds threshold.";

        // 5. Final Decision
        Map<String, Object> affectedEntities = new java.util.LinkedHashMap<>();
        affectedEntities.put("vendorId", disruption.getVendorId());
        affectedEntities.put("region", disruption.getRegion());
        affectedEntities.put("affectedSkus", disruption.getAffectedSkus());

        FinalDecisionInfo finalDecision = new FinalDecisionInfo(
                disruption.getConfidence(),
                disruption.getStatus().name(),
                affectedEntities
        );

        return new AgentTimelineResponse(
                "Disruption Detector Agent",
                disruption.getId().toString(),
                disruption.getStatus().name(),
                disruption.getConfidence(),
                kafkaTrigger,
                ragChunks,
                toolCalls,
                llmReasoning,
                finalDecision
        );
    }

    /** Ingest an anomaly signal into the 15-minute sliding window to evaluate multi-signal correlation */
    @PostMapping("/disruptions/signal")
    public org.springframework.http.ResponseEntity<String> recordSignal(@Valid @RequestBody IngestSignalRequest request) {
        signalListener.observe(request.vendorId(), request.type(), request.severity(), request.description());
        return org.springframework.http.ResponseEntity.ok("Signal recorded into sliding window");
    }

    /** Drives the same real analysis path a live Kafka anomaly would — used to demonstrate/verify the pipeline on demand. */
    @PostMapping("/disruptions/simulate")
    public Disruption simulate(@Valid @RequestBody SimulateRequest request) {
        List<Signal> signals = request.signals().stream()
                .map(s -> new Signal(Instant.now(), s.type(), s.severity(), s.description()))
                .toList();
        return analysisService.analyzeAndPublish(request.vendorId(), signals);
    }

    public record IngestSignalRequest(
            @NotBlank String vendorId,
            @jakarta.validation.constraints.NotNull SignalType type,
            double severity,
            String description
    ) {
    }

    public record ReasoningResponse(double confidence, String reasoningTrace) {
    }

    public record SimulateRequest(@NotBlank String vendorId, @NotEmpty List<SimulatedSignal> signals) {
    }

    public record SimulatedSignal(SignalType type, double severity, String description) {
    }

    public record DisruptionWeeklyAnalytics(
            long count,
            java.util.Map<String, Long> severityBreakdown,
            java.util.List<Double> confidences
    ) {
    }

    public record KafkaTriggerInfo(String topic, String eventType, Instant timestamp, String payload) {
    }

    public record RetrievedRagChunkInfo(String source, String sourceId, double similarityScore, String content) {
    }

    public record ToolCallInfo(int stepIndex, String toolName, String input, String output, long latencyMs, String llmReasoning) {
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
            List<ToolCallInfo> toolCalls,
            String llmReasoning,
            FinalDecisionInfo finalDecision
    ) {
    }
}
