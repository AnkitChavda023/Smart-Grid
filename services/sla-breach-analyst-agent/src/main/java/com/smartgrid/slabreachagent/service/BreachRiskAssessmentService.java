package com.smartgrid.slabreachagent.service;

import com.smartgrid.commons.metrics.AgentMetricsRecorder;
import com.smartgrid.commons.util.Confidence;
import com.smartgrid.slabreachagent.client.InventoryClient;
import com.smartgrid.slabreachagent.client.McpClient;
import com.smartgrid.slabreachagent.client.RagClient;
import com.smartgrid.slabreachagent.client.VendorClient;
import com.smartgrid.slabreachagent.config.SlaBreachAnalystProperties;
import com.smartgrid.slabreachagent.domain.AssessmentStatus;
import com.smartgrid.slabreachagent.domain.BreachRiskAssessment;
import com.smartgrid.slabreachagent.llm.BreachRiskAnalysis;
import com.smartgrid.slabreachagent.llm.BreachRiskAnalyzer;
import com.smartgrid.slabreachagent.messaging.PreemptiveRestockEventPublisher;
import com.smartgrid.slabreachagent.messaging.SlaRiskReportEventPublisher;
import com.smartgrid.slabreachagent.repository.BreachRiskAssessmentRepository;
import io.micrometer.core.instrument.Timer;
import org.springframework.core.ParameterizedTypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class BreachRiskAssessmentService {

    private static final Logger log = LoggerFactory.getLogger(BreachRiskAssessmentService.class);

    private final VendorClient vendorClient;
    private final InventoryClient inventoryClient;
    private final RagClient ragClient;
    private final McpClient mcpClient;
    private final BreachRiskAnalyzer analyzer;
    private final PoissonBreachAnalyzer poissonAnalyzer;
    private final BreachRiskAssessmentRepository assessmentRepository;
    private final SlaRiskReportEventPublisher riskReportPublisher;
    private final PreemptiveRestockEventPublisher restockPublisher;
    private final SlaBreachAnalystProperties properties;
    private final AgentMetricsRecorder metrics;

    public BreachRiskAssessmentService(VendorClient vendorClient, InventoryClient inventoryClient, RagClient ragClient,
                                        McpClient mcpClient, BreachRiskAnalyzer analyzer, PoissonBreachAnalyzer poissonAnalyzer,
                                        BreachRiskAssessmentRepository assessmentRepository,
                                        SlaRiskReportEventPublisher riskReportPublisher,
                                        PreemptiveRestockEventPublisher restockPublisher,
                                        SlaBreachAnalystProperties properties, AgentMetricsRecorder metrics) {
        this.vendorClient = vendorClient;
        this.inventoryClient = inventoryClient;
        this.ragClient = ragClient;
        this.mcpClient = mcpClient;
        this.analyzer = analyzer;
        this.poissonAnalyzer = poissonAnalyzer;
        this.assessmentRepository = assessmentRepository;
        this.riskReportPublisher = riskReportPublisher;
        this.restockPublisher = restockPublisher;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Transactional
    public BreachRiskAssessment assess(String vendorId) {
        Timer.Sample latency = metrics.startLatency();
        try {
            metrics.recordToolCall("getBreachHistory");
            List<BreachRecord> allBreaches = mcpClient.call("getBreachHistory", 1, Map.of("vendorId", vendorId),
                    new ParameterizedTypeReference<List<BreachRecord>>() {
                    });
            Instant windowStart = Instant.now().minus(properties.rollingWindowDays(), ChronoUnit.DAYS);
            int breachCountInWindow = (int) allBreaches.stream()
                    .filter(b -> b.detectedAt() != null && b.detectedAt().isAfter(windowStart))
                    .count();

            double breachProbability = poissonAnalyzer.breachProbability(
                    breachCountInWindow, properties.rollingWindowDays(), properties.forecastHorizonDays());

            metrics.recordToolCall("getVendorRiskProfile");
            Object riskProfile = mcpClient.call("getVendorRiskProfile", 1, Map.of("vendorId", vendorId), Object.class);

            BreachRiskAnalysis analysis = analyzeWithFallback(vendorId, breachProbability, breachCountInWindow, riskProfile);
            double confidence = Confidence.clamp(analysis.confidence());
            metrics.recordConfidence(confidence);

            AssessmentStatus status = confidence >= properties.confidenceThreshold()
                    ? AssessmentStatus.PUBLISHED : AssessmentStatus.PENDING_REVIEW;

            boolean restockTriggered = breachProbability > properties.breachProbabilityThreshold();
            if (restockTriggered) {
                restockTriggered = triggerPreemptiveRestockForVendor(vendorId, breachProbability);
            }

            BreachRiskAssessment assessment = new BreachRiskAssessment(UUID.randomUUID(), vendorId, breachCountInWindow,
                    breachProbability, confidence, analysis.summary(), restockTriggered, status);
            assessmentRepository.save(assessment);

            if (status == AssessmentStatus.PUBLISHED) {
                riskReportPublisher.publish(assessment);
            } else {
                metrics.recordEscalation();
            }

            return assessment;
        } finally {
            metrics.stopLatency(latency);
        }
    }

    /** Restocks every SKU this vendor supplies where the most at-risk warehouse is genuinely under the target level; returns whether any real restock fired. */
    private boolean triggerPreemptiveRestockForVendor(String vendorId, double breachProbability) {
        List<String> skuIds = vendorClient.skuIdsFor(vendorId);
        boolean anyTriggered = false;
        for (String skuId : skuIds) {
            Optional<InventoryClient.WarehouseSnapshot> snapshot = inventoryClient.mostAtRiskWarehouse(skuId);
            if (snapshot.isEmpty()) {
                continue;
            }
            long target = Math.max(properties.targetStockLevel(), snapshot.get().safetyStockLevel());
            long deficit = target - snapshot.get().availableQuantity();
            if (deficit <= 0) {
                continue;
            }
            String warehouseId = snapshot.get().warehouseId();
            String reason = "Predicted SLA breach probability " + String.format("%.2f", breachProbability)
                    + " for vendor " + vendorId + " exceeds threshold";

            metrics.recordToolCall("triggerPreemptiveRestock");
            mcpClient.call("triggerPreemptiveRestock", 1, Map.of(
                    "skuId", skuId, "warehouseId", warehouseId, "quantity", deficit), Object.class);
            restockPublisher.publish(skuId, warehouseId, deficit, reason);

            metrics.recordToolCall("notifyProcurementManager");
            mcpClient.call("notifyProcurementManager", 1, Map.of(
                    "title", "Preemptive restock triggered for vendor " + vendorId,
                    "body", reason + ". SKU " + skuId + " replenished by " + deficit + " units at warehouse " + warehouseId + ".",
                    "relatedEntityId", vendorId), Object.class);

            anyTriggered = true;
        }
        return anyTriggered;
    }

    private BreachRiskAnalysis analyzeWithFallback(String vendorId, double breachProbability, int breachCount, Object riskProfile) {
        List<String> ragContext = searchRagSafely("vendor " + vendorId + " SLA breach pattern contract terms", 3);
        String prompt = """
                Vendor: %s
                Computed Poisson breach probability over next %d days: %.4f (from %d breaches observed in the last %d days)
                Vendor risk profile: %s

                Similar historical context (top matches):
                %s
                """.formatted(vendorId, properties.forecastHorizonDays(), breachProbability, breachCount,
                properties.rollingWindowDays(), riskProfile,
                ragContext.isEmpty() ? "(none retrieved)" : String.join("\n---\n", ragContext));

        try {
            return analyzer.analyze(prompt);
        } catch (Exception first) {
            log.warn("Breach risk LLM call failed for vendorId={}, retrying once: {}", vendorId, first.getMessage());
            try {
                return analyzer.analyze(prompt);
            } catch (Exception second) {
                log.warn("Retry also failed for vendorId={}, falling back to rule-based heuristic: {}", vendorId, second.getMessage());
                return ruleBasedFallback(breachProbability, breachCount);
            }
        }
    }

    /** See DisruptionAnalysisService.searchRagSafely for why this is needed: RAG search can fail for the
     * same OpenAI-billing reason the LLM call above already has a fallback for, but it runs before that
     * fallback chain, so an unguarded failure here would crash the whole assessment instead of degrading. */
    private List<String> searchRagSafely(String query, int k) {
        try {
            return ragClient.search(query, k);
        } catch (Exception e) {
            log.warn("RAG search failed, proceeding with no historical context: {}", e.getMessage());
            return List.of();
        }
    }

    /** Confidence tracks how much real evidence backs the Poisson estimate — more observed breaches means a steadier rate estimate. */
    private BreachRiskAnalysis ruleBasedFallback(double breachProbability, int breachCount) {
        double confidence = Math.min(0.55, breachCount * 0.10);
        String summary = "Rule-based fallback (LLM unavailable): Poisson breach probability=" +
                String.format("%.2f", breachProbability) + " from " + breachCount + " observed breaches";
        return new BreachRiskAnalysis(confidence, summary);
    }

    public record BreachRecord(UUID id, String vendorId, UUID contractId, String severity,
                                double penaltyAmount, String reason, Instant detectedAt) {
    }
}
