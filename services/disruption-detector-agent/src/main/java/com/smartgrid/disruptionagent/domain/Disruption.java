package com.smartgrid.disruptionagent.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "disruptions")
public class Disruption {

    @Id
    private UUID id;

    @Column(name = "vendor_id", nullable = false)
    private String vendorId;

    @Column(nullable = false)
    private String region;

    @ElementCollection
    @CollectionTable(name = "disruption_affected_skus", joinColumns = @jakarta.persistence.JoinColumn(name = "disruption_id"))
    @Column(name = "sku_id")
    private List<String> affectedSkus;

    @Column(nullable = false)
    private double confidence;

    @Column(name = "reasoning_trace", columnDefinition = "text", nullable = false)
    private String reasoningTrace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisruptionStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Disruption() {
    }

    public Disruption(UUID id, String vendorId, String region, List<String> affectedSkus,
                       double confidence, String reasoningTrace, DisruptionStatus status) {
        this.id = id;
        this.vendorId = vendorId;
        this.region = region;
        this.affectedSkus = affectedSkus;
        this.confidence = confidence;
        this.reasoningTrace = reasoningTrace;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getVendorId() {
        return vendorId;
    }

    public String getRegion() {
        return region;
    }

    public List<String> getAffectedSkus() {
        return affectedSkus;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getReasoningTrace() {
        return reasoningTrace;
    }

    public DisruptionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
