package com.smartgrid.vendorservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Vendor health report computed during vendor evaluations. */
@Entity
@Table(name = "vendor_health_reports")
public class VendorHealthReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "trend_direction", nullable = false)
    private String trendDirection;

    @Column(name = "trend_slope", nullable = false)
    private double trendSlope;

    @Column(name = "average_lead_time_days", nullable = false)
    private double averageLeadTimeDays;

    @Column(name = "breach_count", nullable = false)
    private int breachCount;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected VendorHealthReport() {
    }

    public VendorHealthReport(UUID vendorId, String trendDirection, double trendSlope,
                               double averageLeadTimeDays, int breachCount, String summary) {
        this.vendorId = vendorId;
        this.trendDirection = trendDirection;
        this.trendSlope = trendSlope;
        this.averageLeadTimeDays = averageLeadTimeDays;
        this.breachCount = breachCount;
        this.summary = summary;
    }

    public UUID getId() {
        return id;
    }

    public UUID getVendorId() {
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

    public String getSummary() {
        return summary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
