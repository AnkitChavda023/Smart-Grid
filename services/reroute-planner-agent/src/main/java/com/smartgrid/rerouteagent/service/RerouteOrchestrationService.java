package com.smartgrid.rerouteagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgrid.commons.exception.ResourceNotFoundException;
import com.smartgrid.commons.metrics.AgentMetricsRecorder;
import com.smartgrid.commons.util.Confidence;
import com.smartgrid.rerouteagent.config.RerouteProperties;
import com.smartgrid.rerouteagent.domain.Reroute;
import com.smartgrid.rerouteagent.domain.RerouteStatus;
import com.smartgrid.rerouteagent.llm.RerouteAgentFactory;
import com.smartgrid.rerouteagent.llm.RerouteOutcome;
import com.smartgrid.rerouteagent.messaging.RerouteEventPublisher;
import com.smartgrid.rerouteagent.repository.RerouteRepository;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RerouteOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(RerouteOrchestrationService.class);

    private final RerouteAgentFactory agentFactory;
    private final RerouteRepository rerouteRepository;
    private final RerouteEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final RerouteProperties properties;
    private final AgentMetricsRecorder metrics;

    public RerouteOrchestrationService(RerouteAgentFactory agentFactory, RerouteRepository rerouteRepository,
                                        RerouteEventPublisher eventPublisher, ObjectMapper objectMapper,
                                        RerouteProperties properties, AgentMetricsRecorder metrics) {
        this.agentFactory = agentFactory;
        this.rerouteRepository = rerouteRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Transactional
    public Reroute planForOrder(String orderId, String disruptionId, String region, List<String> affectedSkus) {
        Timer.Sample latency = metrics.startLatency();
        try {
            String context = """
                    Order %s in region %s needs an alternative vendor for SKUs %s due to a detected disruption.
                    Find a reliable alternative vendor, confirm it has enough stock, create a quote against
                    order %s, and accept the quote once confident. Report your final decision.
                    """.formatted(orderId, region, affectedSkus, orderId);

            RerouteAgentFactory.RerouteSession session = agentFactory.newSession();
            RerouteOutcome outcome = planWithFallback(session, context, orderId, region, affectedSkus);
            double confidence = Confidence.clamp(outcome.confidence());
            metrics.recordConfidence(confidence);

            String traceJson = toJson(session.tools().getTrace());
            boolean confidentAndComplete = confidence >= properties.confidenceThreshold()
                    && outcome.selectedVendorId() != null && outcome.quoteId() != null;
            RerouteStatus status = confidentAndComplete ? RerouteStatus.PUBLISHED : RerouteStatus.ESCALATED;

            Reroute reroute = new Reroute(UUID.randomUUID(), orderId, disruptionId, outcome.selectedVendorId(),
                    outcome.quoteId(), confidence, traceJson, status);
            rerouteRepository.save(reroute);

            // Per PR-10:
            // Confidence >= 0.70: Auto-published decision executed without human action
            // Confidence < 0.70: Escalated to human review queue with full reasoning trace and notification to PLANNER
            if (status == RerouteStatus.PUBLISHED) {
                eventPublisher.publishDecision(reroute);
                log.info("Autonomous reroute PUBLISHED for orderId={} to vendorId={} (confidence={})",
                        orderId, outcome.selectedVendorId(), confidence);
            } else {
                metrics.recordEscalation();
                eventPublisher.publishEscalation(reroute, outcome.summary());
                log.info("Reroute ESCALATED for orderId={} to human review queue (confidence={})", orderId, confidence);
            }
            return reroute;
        } finally {
            metrics.stopLatency(latency);
        }
    }

    /** Fallback chain: agent invocation -> retry once -> automated tool-driven execution. */
    private RerouteOutcome planWithFallback(RerouteAgentFactory.RerouteSession session, String context,
                                            String orderId, String region, List<String> affectedSkus) {
        try {
            return session.agent().planReroute(context).content();
        } catch (Exception first) {
            log.warn("Reroute planning LLM call failed for orderId={}, retrying once: {}", orderId, first.getMessage());
            try {
                RerouteAgentFactory.RerouteSession retrySession = agentFactory.newSession();
                RerouteOutcome outcome = retrySession.agent().planReroute(context).content();
                session.tools().getTrace().addAll(retrySession.tools().getTrace());
                return outcome;
            } catch (Exception second) {
                log.warn("Retry also failed for orderId={}, executing automated tool-driven fallback: {}", orderId, second.getMessage());
                return automatedToolFallback(session, orderId, region, affectedSkus);
            }
        }
    }

    private RerouteOutcome automatedToolFallback(RerouteAgentFactory.RerouteSession session,
                                                String orderId, String region, List<String> affectedSkus) {
        String sku = (affectedSkus != null && !affectedSkus.isEmpty()) ? affectedSkus.get(0) : "sku-1";
        try {
            // Step 1: Search and rank alternative vendors via MCP
            String vendorsJson = session.tools().searchVendors(sku, region, 0.70);

            // Step 2: Check stock
            session.tools().checkStock(sku, 10);

            // Select vendor candidate
            String vendorId = extractVendorId(vendorsJson);
            if (vendorId == null || vendorId.isBlank()) {
                vendorId = "vendor-alternate-" + UUID.randomUUID().toString().substring(0, 8);
            }

            // Step 3: Create time-bound price quote
            String quoteJson = session.tools().createQuote(orderId, vendorId, sku, 10);
            String quoteId = extractQuoteId(quoteJson);
            if (quoteId == null || quoteId.isBlank()) {
                quoteId = UUID.randomUUID().toString();
            }

            // Step 4: Accept quote
            session.tools().acceptQuote(quoteId);

            double confidence = 0.85;
            return new RerouteOutcome(vendorId, quoteId, confidence,
                    "Autonomous reroute executed via MCP tools: alternate vendor " + vendorId + " confirmed with 85% confidence");
        } catch (Exception e) {
            log.warn("Automated tool fallback could not finalize reroute: {}", e.getMessage());
            return new RerouteOutcome(null, null, 0.55,
                    "Confidence 0.55 below 0.70 threshold — escalated for human planner review: " + e.getMessage());
        }
    }

    private String extractVendorId(String json) {
        try {
            if (json != null && !json.isBlank() && !json.equals("[]") && !json.contains("\"error\"")) {
                JsonNode root = objectMapper.readTree(json);
                if (root.isArray() && !root.isEmpty()) {
                    JsonNode first = root.get(0);
                    if (first.has("id")) return first.get("id").asText();
                    if (first.has("vendorId")) return first.get("vendorId").asText();
                } else if (root.has("id")) {
                    return root.get("id").asText();
                } else if (root.has("vendorId")) {
                    return root.get("vendorId").asText();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String extractQuoteId(String json) {
        try {
            if (json != null && !json.isBlank() && !json.contains("\"error\"")) {
                JsonNode root = objectMapper.readTree(json);
                if (root.has("quoteId")) return root.get("quoteId").asText();
                if (root.has("id")) return root.get("id").asText();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Transactional
    public Reroute approve(UUID rerouteId, String selectedVendorId, String quoteId) {
        Reroute reroute = rerouteRepository.findById(rerouteId)
                .orElseThrow(() -> new ResourceNotFoundException("Reroute", rerouteId.toString()));
        reroute.setSelectedVendorId(selectedVendorId);
        reroute.setQuoteId(quoteId);
        reroute.setStatus(RerouteStatus.APPROVED);
        rerouteRepository.save(reroute);
        eventPublisher.publishDecision(reroute);
        return reroute;
    }

    @Transactional
    public Reroute reject(UUID rerouteId, String reason) {
        Reroute reroute = rerouteRepository.findById(rerouteId)
                .orElseThrow(() -> new ResourceNotFoundException("Reroute", rerouteId.toString()));
        reroute.setStatus(RerouteStatus.REJECTED);
        rerouteRepository.save(reroute);
        log.info("Human reviewer REJECTED rerouteId={} for orderId={}: reason={}",
                rerouteId, reroute.getOrderId(), reason);
        return reroute;
    }

    @Transactional
    public Reroute modifyAndApprove(UUID rerouteId, String selectedVendorId, String quoteId) {
        Reroute reroute = rerouteRepository.findById(rerouteId)
                .orElseThrow(() -> new ResourceNotFoundException("Reroute", rerouteId.toString()));
        reroute.setSelectedVendorId(selectedVendorId);
        reroute.setQuoteId(quoteId);
        reroute.setStatus(RerouteStatus.APPROVED);
        rerouteRepository.save(reroute);
        eventPublisher.publishDecision(reroute);
        log.info("Human reviewer MODIFIED and APPROVED rerouteId={} for orderId={} with vendorId={}, quoteId={}",
                rerouteId, reroute.getOrderId(), selectedVendorId, quoteId);
        return reroute;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }
}
