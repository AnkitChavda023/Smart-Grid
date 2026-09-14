package com.smartgrid.vendorevaluatoragent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vendor_evaluations")
public class VendorEvaluation {

    @Id
    private UUID id;

    @Column(name = "vendor_id", nullable = false)
    private String vendorId;

    @Column(name = "trend_direction", nullable = false)
    private String trendDirection;

    @Column(name = "trend_slope", nullable = false)
    private double trendSlope;

    @Column(name = "average_lead_time_days", nullable = false)
    private double averageLeadTimeDays;

    @Column(name = "breach_count", nullable = false)
    private int breachCount;

    @Column(nullable = false)
    private double confidence;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvaluationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected VendorEvaluation() {
    }

    public VendorEvaluation(UUID id, String vendorId, String trendDirection, double trendSlope,
                             double averageLeadTimeDays, int breachCount, double confidence,
                             String summary, EvaluationStatus status) {
        this.id = id;
        this.vendorId = vendorId;
        this.trendDirection = trendDirection;
        this.trendSlope = trendSlope;
        this.averageLeadTimeDays = averageLeadTimeDays;
        this.breachCount = breachCount;
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

    public String getTrendDirection() {
        return trendDirection;
    }

    public double getTrendSlope() {
        return trendSlope;
    }

    public double getAverageLeadTimeDays() {
        return averageLeadTimeDays;
    }

    public int getBreachCount() {
        return breachCount;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getSummary() {
        return summary;
    }

    public EvaluationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
