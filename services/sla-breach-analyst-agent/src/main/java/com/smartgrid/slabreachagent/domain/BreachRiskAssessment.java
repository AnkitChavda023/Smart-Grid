package com.smartgrid.slabreachagent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "breach_risk_assessments")
public class BreachRiskAssessment {

    @Id
    private UUID id;

    @Column(name = "vendor_id", nullable = false)
    private String vendorId;

    @Column(name = "breach_count_in_window", nullable = false)
    private int breachCountInWindow;

    @Column(name = "breach_probability", nullable = false)
    private double breachProbability;

    @Column(nullable = false)
    private double confidence;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(name = "restock_triggered", nullable = false)
    private boolean restockTriggered;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssessmentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected BreachRiskAssessment() {
    }

    public BreachRiskAssessment(UUID id, String vendorId, int breachCountInWindow, double breachProbability,
                                 double confidence, String summary, boolean restockTriggered, AssessmentStatus status) {
        this.id = id;
        this.vendorId = vendorId;
        this.breachCountInWindow = breachCountInWindow;
        this.breachProbability = breachProbability;
        this.confidence = confidence;
        this.summary = summary;
        this.restockTriggered = restockTriggered;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public String getVendorId() {
        return vendorId;
    }

    public int getBreachCountInWindow() {
        return breachCountInWindow;
    }

    public double getBreachProbability() {
        return breachProbability;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getSummary() {
        return summary;
    }

    public boolean isRestockTriggered() {
        return restockTriggered;
    }

    public AssessmentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
