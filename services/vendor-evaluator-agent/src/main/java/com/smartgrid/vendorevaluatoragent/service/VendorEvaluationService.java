package com.smartgrid.vendorevaluatoragent.service;

import com.smartgrid.commons.metrics.AgentMetricsRecorder;
import com.smartgrid.commons.util.Confidence;
import com.smartgrid.vendorevaluatoragent.client.McpClient;
import com.smartgrid.vendorevaluatoragent.client.RagClient;
import com.smartgrid.vendorevaluatoragent.client.VendorClient;
import com.smartgrid.vendorevaluatoragent.config.VendorEvaluatorProperties;
import com.smartgrid.vendorevaluatoragent.domain.EvaluationStatus;
import com.smartgrid.vendorevaluatoragent.domain.VendorEvaluation;
import com.smartgrid.vendorevaluatoragent.domain.VendorLeadTimeObservation;
import com.smartgrid.vendorevaluatoragent.llm.VendorEvaluationAnalysis;
import com.smartgrid.vendorevaluatoragent.llm.VendorEvaluationAnalyzer;
import com.smartgrid.vendorevaluatoragent.messaging.VendorHealthReportEventPublisher;
import com.smartgrid.vendorevaluatoragent.repository.VendorEvaluationRepository;
import com.smartgrid.vendorevaluatoragent.repository.VendorLeadTimeObservationRepository;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VendorEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(VendorEvaluationService.class);

    private final VendorClient vendorClient;
    private final RagClient ragClient;
    private final McpClient mcpClient;
    private final VendorEvaluationAnalyzer analyzer;
    private final LeadTimeTrendAnalyzer trendAnalyzer;
    private final VendorLeadTimeObservationRepository observationRepository;
    private final VendorEvaluationRepository evaluationRepository;
    private final VendorHealthReportEventPublisher eventPublisher;
    private final VendorEvaluatorProperties properties;
    private final AgentMetricsRecorder metrics;

    public VendorEvaluationService(VendorClient vendorClient, RagClient ragClient, McpClient mcpClient,
                                    VendorEvaluationAnalyzer analyzer, LeadTimeTrendAnalyzer trendAnalyzer,
                                    VendorLeadTimeObservationRepository observationRepository,
                                    VendorEvaluationRepository evaluationRepository,
                                    VendorHealthReportEventPublisher eventPublisher,
                                    VendorEvaluatorProperties properties, AgentMetricsRecorder metrics) {
        this.vendorClient = vendorClient;
        this.ragClient = ragClient;
        this.mcpClient = mcpClient;
        this.analyzer = analyzer;
        this.trendAnalyzer = trendAnalyzer;
        this.observationRepository = observationRepository;
        this.evaluationRepository = evaluationRepository;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Transactional
    public VendorEvaluation evaluate(String vendorId) {
        Timer.Sample latency = metrics.startLatency();
        try {
            double currentLeadTime = vendorClient.currentAverageLeadTimeDays(vendorId);
            observationRepository.save(new VendorLeadTimeObservation(vendorId, currentLeadTime));

            Instant since = Instant.now().minus(properties.trendWindowDays(), ChronoUnit.DAYS);
            List<VendorLeadTimeObservation> observations =
                    observationRepository.findByVendorIdAndObservedAtAfterOrderByObservedAtAsc(vendorId, since);
            LeadTimeTrendAnalyzer.TrendResult trend = trendAnalyzer.analyze(observations);

            metrics.recordToolCall("getVendorHistory");
            Object vendorHistory = mcpClient.call("getVendorHistory", 1, Map.of("vendorId", vendorId), Object.class);
            metrics.recordToolCall("getBreachHistory");
            List<?> breachHistory = mcpClient.call("getBreachHistory", 1, Map.of("vendorId", vendorId), List.class);
            int breachCount = breachHistory == null ? 0 : breachHistory.size();

            VendorEvaluationAnalysis analysis = analyzeWithFallback(vendorId, trend, currentLeadTime, breachCount, vendorHistory);
            double confidence = Confidence.clamp(analysis.confidence());
            metrics.recordConfidence(confidence);

            EvaluationStatus status = confidence >= properties.confidenceThreshold()
                    ? EvaluationStatus.PUBLISHED : EvaluationStatus.PENDING_REVIEW;

            VendorEvaluation evaluation = new VendorEvaluation(UUID.randomUUID(), vendorId, trend.direction(),
                    trend.slope(), currentLeadTime, breachCount, confidence, analysis.summary(), status);
            evaluationRepository.save(evaluation);

            if (status == EvaluationStatus.PUBLISHED) {
                metrics.recordToolCall("updateVendorHealthReport");
                mcpClient.call("updateVendorHealthReport", 1, Map.of(
                        "vendorId", vendorId,
                        "trendDirection", trend.direction(),
                        "trendSlope", trend.slope(),
                        "averageLeadTimeDays", currentLeadTime,
                        "breachCount", breachCount,
                        "summary", analysis.summary()), Object.class);
                eventPublisher.publish(evaluation);
            } else {
                metrics.recordEscalation();
            }

            return evaluation;
        } finally {
            metrics.stopLatency(latency);
        }
    }

    private VendorEvaluationAnalysis analyzeWithFallback(String vendorId, LeadTimeTrendAnalyzer.TrendResult trend,
                                                           double currentLeadTime, int breachCount, Object vendorHistory) {
        List<String> ragContext = searchRagSafely("vendor " + vendorId + " performance breaches capabilities", 5);
        String prompt = """
                Vendor: %s
                Lead time trend: %s (slope %.4f days/day), current average lead time %.1f days
                SLA breach count: %d
                Vendor performance history: %s

                Relevant historical context:
                %s
                """.formatted(vendorId, trend.direction(), trend.slope(), currentLeadTime, breachCount, vendorHistory,
                ragContext.isEmpty() ? "(none retrieved)" : String.join("\n---\n", ragContext));

        try {
            return analyzer.analyze(prompt);
        } catch (Exception first) {
            log.warn("Vendor evaluation LLM call failed for vendorId={}, retrying once: {}", vendorId, first.getMessage());
            try {
                return analyzer.analyze(prompt);
            } catch (Exception second) {
                log.warn("Retry also failed for vendorId={}, falling back to rule-based heuristic: {}", vendorId, second.getMessage());
                return ruleBasedFallback(trend, breachCount);
            }
        }
    }

    /** See DisruptionAnalysisService.searchRagSafely for why this is needed: RAG search can fail for the
     * same OpenAI-billing reason the LLM call above already has a fallback for, but it runs before that
     * fallback chain, so an unguarded failure here would crash the whole evaluation instead of degrading. */
    private List<String> searchRagSafely(String query, int k) {
        try {
            return ragClient.search(query, k);
        } catch (Exception e) {
            log.warn("RAG search failed, proceeding with no historical context: {}", e.getMessage());
            return List.of();
        }
    }

    /** Confidence scales down with breach count and a worsening trend, capped below the auto-publish threshold. */
    private VendorEvaluationAnalysis ruleBasedFallback(LeadTimeTrendAnalyzer.TrendResult trend, int breachCount) {
        double base = "DECLINING".equals(trend.direction()) ? 0.30 : 0.50;
        double confidence = Math.max(0.0, base - (breachCount * 0.05));
        String summary = "Rule-based fallback (LLM unavailable): trend=" + trend.direction() + ", breachCount=" + breachCount;
        return new VendorEvaluationAnalysis(confidence, summary);
    }
}
