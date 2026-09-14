package com.smartgrid.contractagent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "negotiation_runs")
public class NegotiationRun {

    @Id
    private UUID id;

    @Column(name = "vendor_id", nullable = false)
    private String vendorId;

    @Column(name = "draft_id")
    private String draftId;

    @Column(name = "clause_similarity", nullable = false)
    private double clauseSimilarity;

    @Column(nullable = false)
    private double confidence;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NegotiationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected NegotiationRun() {
    }

    public NegotiationRun(UUID id, String vendorId, String draftId, double clauseSimilarity,
                           double confidence, String summary, NegotiationStatus status) {
        this.id = id;
        this.vendorId = vendorId;
        this.draftId = draftId;
        this.clauseSimilarity = clauseSimilarity;
        this.confidence = confidence;
        this.summary = summary;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public String getVendorId() {
        return vendorId;
    }

    public String getDraftId() {
        return draftId;
    }

    public double getClauseSimilarity() {
        return clauseSimilarity;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getSummary() {
        return summary;
    }

    public NegotiationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
