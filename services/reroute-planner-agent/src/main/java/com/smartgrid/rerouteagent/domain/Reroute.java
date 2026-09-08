package com.smartgrid.rerouteagent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reroutes")
public class Reroute {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "disruption_id")
    private String disruptionId;

    @Column(name = "selected_vendor_id")
    private String selectedVendorId;

    @Column(name = "quote_id")
    private String quoteId;

    @Column(nullable = false)
    private double confidence;

    /** Serialized {@code List<DagTraceNode>} — the same JSON string published on RerouteDecision.agentTrace. */
    @Column(name = "agent_trace_json", columnDefinition = "text", nullable = false)
    private String agentTraceJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RerouteStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Reroute() {
    }

    public Reroute(UUID id, String orderId, String disruptionId, String selectedVendorId, String quoteId,
                    double confidence, String agentTraceJson, RerouteStatus status) {
        this.id = id;
        this.orderId = orderId;
        this.disruptionId = disruptionId;
        this.selectedVendorId = selectedVendorId;
        this.quoteId = quoteId;
        this.confidence = confidence;
        this.agentTraceJson = agentTraceJson;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getDisruptionId() {
        return disruptionId;
    }

    public String getSelectedVendorId() {
        return selectedVendorId;
    }

    public String getQuoteId() {
        return quoteId;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getAgentTraceJson() {
        return agentTraceJson;
    }

    public RerouteStatus getStatus() {
        return status;
    }

    public void setStatus(RerouteStatus status) {
        this.status = status;
    }

    public void setSelectedVendorId(String selectedVendorId) {
        this.selectedVendorId = selectedVendorId;
    }

    public void setQuoteId(String quoteId) {
        this.quoteId = quoteId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
