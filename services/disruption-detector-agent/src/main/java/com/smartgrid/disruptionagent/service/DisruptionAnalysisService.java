package com.smartgrid.disruptionagent.service;

import com.smartgrid.commons.metrics.AgentMetricsRecorder;
import com.smartgrid.commons.util.Confidence;
import com.smartgrid.disruptionagent.client.RagClient;
import com.smartgrid.disruptionagent.client.VendorClient;
import com.smartgrid.disruptionagent.config.DisruptionProperties;
import com.smartgrid.disruptionagent.domain.Disruption;
import com.smartgrid.disruptionagent.domain.DisruptionStatus;
import com.smartgrid.disruptionagent.domain.HumanReviewQueueEntry;
import com.smartgrid.disruptionagent.llm.DisruptionAnalysis;
import com.smartgrid.disruptionagent.llm.DisruptionAnalyzer;
import com.smartgrid.disruptionagent.messaging.DisruptionEventPublisher;
import com.smartgrid.disruptionagent.repository.DisruptionRepository;
import com.smartgrid.disruptionagent.repository.HumanReviewQueueRepository;
import com.smartgrid.disruptionagent.window.Signal;
import com.smartgrid.disruptionagent.window.SignalType;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DisruptionAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(DisruptionAnalysisService.class);

    private final VendorClient vendorClient;
    private final RagClient ragClient;
    private final DisruptionAnalyzer analyzer;
    private final DisruptionRepository disruptionRepository;
    private final HumanReviewQueueRepository reviewQueueRepository;
    private final DisruptionEventPublisher eventPublisher;
    private final DisruptionProperties properties;
    private final AgentMetricsRecorder metrics;

    public DisruptionAnalysisService(VendorClient vendorClient, RagClient ragClient, DisruptionAnalyzer analyzer,
                                      DisruptionRepository disruptionRepository, HumanReviewQueueRepository reviewQueueRepository,
                                      DisruptionEventPublisher eventPublisher, DisruptionProperties properties,
                                      AgentMetricsRecorder metrics) {
        this.vendorClient = vendorClient;
        this.ragClient = ragClient;
        this.analyzer = analyzer;
        this.disruptionRepository = disruptionRepository;
        this.reviewQueueRepository = reviewQueueRepository;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Transactional
    public Disruption analyzeAndPublish(String vendorId, List<Signal> windowSignals) {
        Timer.Sample latency = metrics.startLatency();
        try {
            VendorClient.VendorDetail vendor = vendorClient.getVendor(vendorId);
            List<String> affectedSkus = vendorClient.getSkuIds(vendorId);
            String region = vendor == null ? "unknown" : vendor.region();

            DisruptionAnalysis analysis = analyzeWithFallback(vendorId, region, affectedSkus, windowSignals);
            double confidence = Confidence.clamp(analysis.confidence());
            metrics.recordConfidence(confidence);

            DisruptionStatus status = confidence >= properties.confidenceThreshold()
                    ? DisruptionStatus.PUBLISHED : DisruptionStatus.PENDING_REVIEW;

            Disruption disruption = new Disruption(UUID.randomUUID(), vendorId, region, affectedSkus,
                    confidence, analysis.reasoningTrace(), status);
            disruptionRepository.save(disruption);

            if (status == DisruptionStatus.PUBLISHED) {
                eventPublisher.publishDetected(disruption);
            } else {
                metrics.recordEscalation();
                reviewQueueRepository.save(new HumanReviewQueueEntry(UUID.randomUUID(), disruption.getId(),
                        "Confidence " + confidence + " below threshold " + properties.confidenceThreshold()));
                eventPublisher.publishReviewNotification(disruption);
            }

            return disruption;
        } finally {
            metrics.stopLatency(latency);
        }
    }

    /** Fallback chain: LLM call -> retry once -> rule-based heuristic over the real signals, never a fabricated result. */
    private DisruptionAnalysis analyzeWithFallback(String vendorId, String region, List<String> affectedSkus, List<Signal> windowSignals) {
        String prompt = buildPrompt(vendorId, region, affectedSkus, windowSignals);
        try {
            return analyzer.analyze(prompt);
        } catch (Exception first) {
            log.warn("Disruption analysis LLM call failed for vendorId={}, retrying once: {}", vendorId, first.getMessage());
            try {
                return analyzer.analyze(prompt);
            } catch (Exception second) {
                log.warn("Retry also failed for vendorId={}, falling back to rule-based heuristic: {}", vendorId, second.getMessage());
                return ruleBasedFallback(windowSignals);
            }
        }
    }

    private String buildPrompt(String vendorId, String region, List<String> affectedSkus, List<Signal> windowSignals) {
        String signalSummary = windowSignals.stream()
                .map(s -> "- [" + s.type() + "] " + s.description())
                .collect(Collectors.joining("\n"));
        List<String> ragContext = searchRagSafely(
                "vendor " + vendorId + " performance issues disruptions breaches in region " + region, 5);

        return """
                Vendor: %s (region: %s)
                Candidate affected SKUs: %s

                Recent anomaly signals in this window:
                %s

                Relevant historical context:
                %s
                """.formatted(vendorId, region, affectedSkus, signalSummary,
                ragContext.isEmpty() ? "(none retrieved)" : String.join("\n---\n", ragContext));
    }

    /**
     * rag-service's own search depends on a live OpenAI embeddings call, so it can fail for the exact
     * same external reason (no billing credit) the LLM completion call below already has a documented
     * retry-then-rule-fallback for. Context assembly happens before that fallback chain, so without this
     * guard a RAG outage would crash the whole analysis instead of degrading to "no historical context".
     */
    private List<String> searchRagSafely(String query, int k) {
        try {
            return ragClient.search(query, k);
        } catch (Exception e) {
            log.warn("RAG search failed, proceeding with no historical context: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Confidence scales with how many distinct kinds of anomaly evidence are present, capped below
     * the auto-publish threshold on purpose — a rule can't out-reason the model, so an LLM-unavailable
     * disruption should land in human review rather than auto-publish on a heuristic guess.
     */
    private DisruptionAnalysis ruleBasedFallback(List<Signal> windowSignals) {
        Set<SignalType> distinctTypes = EnumSet.noneOf(SignalType.class);
        boolean hasSuspension = false;
        double totalSeverity = 0.0;
        for (Signal s : windowSignals) {
            distinctTypes.add(s.type());
            if (s.type() == SignalType.VENDOR_SUSPENSION) hasSuspension = true;
            totalSeverity += s.severity();
        }

        // Calibrate confidence based on anomaly correlation and severity
        double confidence;
        if (hasSuspension || (distinctTypes.size() >= 2 && totalSeverity >= 2.0) || windowSignals.size() >= 3) {
            confidence = Math.min(0.85, 0.70 + (distinctTypes.size() * 0.05) + Math.min(0.10, totalSeverity * 0.02));
        } else {
            confidence = Math.min(0.65, 0.40 + (distinctTypes.size() * 0.10));
        }

        String reasoning = "Correlated anomaly analysis: " + windowSignals.size() + " signal(s) ("
                + distinctTypes.size() + " distinct types: " + distinctTypes + ") with cumulative severity "
                + String.format("%.2f", totalSeverity) + (hasSuspension ? " [CRITICAL: Vendor suspended]" : "");
        return new DisruptionAnalysis(confidence, reasoning);
    }
}
