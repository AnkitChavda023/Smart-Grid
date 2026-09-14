package com.smartgrid.contractagent.service;

import com.smartgrid.commons.metrics.AgentMetricsRecorder;
import com.smartgrid.commons.util.Confidence;
import com.smartgrid.contractagent.client.McpClient;
import com.smartgrid.contractagent.client.RagClient;
import com.smartgrid.contractagent.config.ContractNegotiationProperties;
import com.smartgrid.contractagent.domain.NegotiationRun;
import com.smartgrid.contractagent.domain.NegotiationStatus;
import com.smartgrid.contractagent.llm.ContractProposal;
import com.smartgrid.contractagent.llm.ContractProposalAnalyzer;
import com.smartgrid.contractagent.messaging.ContractDraftEventPublisher;
import com.smartgrid.contractagent.repository.NegotiationRunRepository;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ContractNegotiationService {

    private static final Logger log = LoggerFactory.getLogger(ContractNegotiationService.class);

    private final McpClient mcpClient;
    private final RagClient ragClient;
    private final ContractProposalAnalyzer analyzer;
    private final ClauseDiffAnalyzer diffAnalyzer;
    private final NegotiationRunRepository runRepository;
    private final ContractDraftEventPublisher draftPublisher;
    private final ContractNegotiationProperties properties;
    private final AgentMetricsRecorder metrics;

    public ContractNegotiationService(McpClient mcpClient, RagClient ragClient, ContractProposalAnalyzer analyzer,
                                       ClauseDiffAnalyzer diffAnalyzer, NegotiationRunRepository runRepository,
                                       ContractDraftEventPublisher draftPublisher,
                                       ContractNegotiationProperties properties, AgentMetricsRecorder metrics) {
        this.mcpClient = mcpClient;
        this.ragClient = ragClient;
        this.analyzer = analyzer;
        this.diffAnalyzer = diffAnalyzer;
        this.runRepository = runRepository;
        this.draftPublisher = draftPublisher;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Transactional
    public NegotiationRun negotiate(String vendorId) {
        Timer.Sample latency = metrics.startLatency();
        try {
            metrics.recordToolCall("getActiveContract");
            List<ActiveContract> contracts = mcpClient.call("getActiveContract", 1, Map.of("vendorId", vendorId),
                    new ParameterizedTypeReference<List<ActiveContract>>() {
                    });
            Optional<ActiveContract> activeContract = contracts.stream().findFirst();
            String currentTerms = activeContract.map(ActiveContract::terms).orElse("");
            String existingContractId = activeContract.map(c -> c.id().toString()).orElse(null);

            metrics.recordToolCall("getBreachHistory");
            Object breachHistory = mcpClient.call("getBreachHistory", 1, Map.of("vendorId", vendorId), Object.class);

            Object benchmarks = fetchBenchmarksForContract(activeContract);

            ContractProposal proposal = proposeWithFallback(vendorId, currentTerms, breachHistory, benchmarks);
            double confidence = Confidence.clamp(proposal.confidence());
            metrics.recordConfidence(confidence);
            double clauseSimilarity = diffAnalyzer.similarity(currentTerms, proposal.proposedTerms());

            NegotiationStatus status = confidence >= properties.confidenceThreshold()
                    ? NegotiationStatus.PUBLISHED : NegotiationStatus.PENDING_REVIEW;

            String draftId = null;
            if (status == NegotiationStatus.PUBLISHED) {
                metrics.recordToolCall("saveDraftContract");
                Map<String, Object> draftParams = new java.util.HashMap<>();
                draftParams.put("vendorId", vendorId);
                if (existingContractId != null) {
                    draftParams.put("existingContractId", existingContractId);
                }
                draftParams.put("proposedTerms", proposal.proposedTerms());
                draftParams.put("summary", proposal.summary());
                DraftResult draft = mcpClient.call("saveDraftContract", 1, draftParams, DraftResult.class);
                draftId = draft.id();

                metrics.recordToolCall("notifyPlannerForReview");
                mcpClient.call("notifyPlannerForReview", 1, Map.of(
                        "title", "Contract draft ready for review: vendor " + vendorId,
                        "body", proposal.summary(),
                        "relatedEntityId", draftId), Object.class);

                draftPublisher.publish(draftId, vendorId, proposal.summary());
            } else {
                metrics.recordEscalation();
            }

            NegotiationRun run = new NegotiationRun(UUID.randomUUID(), vendorId, draftId, clauseSimilarity,
                    confidence, proposal.summary(), status);
            runRepository.save(run);
            return run;
        } finally {
            metrics.stopLatency(latency);
        }
    }

    /** Uses the first SLA metric named on the vendor's active contract as the real, non-fabricated basis for a benchmark comparison. */
    private Object fetchBenchmarksForContract(Optional<ActiveContract> activeContract) {
        Optional<String> metricName = activeContract
                .map(ActiveContract::slaTerms)
                .filter(terms -> !terms.isEmpty())
                .map(terms -> terms.get(0).get("metricName"))
                .filter(String.class::isInstance)
                .map(String.class::cast);

        if (metricName.isEmpty()) {
            return Map.of();
        }
        metrics.recordToolCall("getMarketBenchmarks");
        return mcpClient.call("getMarketBenchmarks", 1, Map.of("metricName", metricName.get()), Object.class);
    }

    private ContractProposal proposeWithFallback(String vendorId, String currentTerms, Object breachHistory, Object benchmarks) {
        List<String> ragContext = searchRagSafely("vendor " + vendorId + " contract terms breach history", 3);
        String prompt = """
                Vendor: %s
                Current contract terms: %s

                Real SLA breach history: %s
                Real market benchmark averages: %s

                Relevant historical context:
                %s
                """.formatted(vendorId, currentTerms.isBlank() ? "(no active contract on file)" : currentTerms,
                breachHistory, benchmarks,
                ragContext.isEmpty() ? "(none retrieved)" : String.join("\n---\n", ragContext));

        try {
            return analyzer.propose(prompt);
        } catch (Exception first) {
            log.warn("Contract proposal LLM call failed for vendorId={}, retrying once: {}", vendorId, first.getMessage());
            try {
                return analyzer.propose(prompt);
            } catch (Exception second) {
                log.warn("Retry also failed for vendorId={}, falling back to unchanged terms: {}", vendorId, second.getMessage());
                return new ContractProposal(currentTerms,
                        "Rule-based fallback (LLM unavailable): proposing no change to current terms pending human review", 0.30);
            }
        }
    }

    /** See DisruptionAnalysisService.searchRagSafely for why this is needed: RAG search can fail for the
     * same OpenAI-billing reason the LLM call above already has a fallback for, but it runs before that
     * fallback chain, so an unguarded failure here would crash the whole negotiation instead of degrading. */
    private List<String> searchRagSafely(String query, int k) {
        try {
            return ragClient.search(query, k);
        } catch (Exception e) {
            log.warn("RAG search failed, proceeding with no historical context: {}", e.getMessage());
            return List.of();
        }
    }

    public record ActiveContract(UUID id, String vendorId, String terms, String startDate, String endDate,
                                  boolean active, List<Map<String, Object>> slaTerms) {
    }

    private record DraftResult(String id, String status) {
    }
}
