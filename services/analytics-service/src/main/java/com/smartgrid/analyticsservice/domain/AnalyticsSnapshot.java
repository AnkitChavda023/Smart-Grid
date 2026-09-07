package com.smartgrid.analyticsservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analytics_snapshots")
public class AnalyticsSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String metricType;

    @Column(nullable = false)
    private String windowType;

    @Column(nullable = false)
    private long windowStart;

    @Column(nullable = false)
    private long windowEnd;

    @Column(nullable = false)
    private double value;

    @Column(nullable = false)
    private String dimensionsJson;

    @Column(nullable = false)
    private Instant capturedAt;

    protected AnalyticsSnapshot() {
    }

    public AnalyticsSnapshot(String metricType, String windowType, long windowStart, long windowEnd,
            double value, String dimensionsJson) {
        this.metricType = metricType;
        this.windowType = windowType;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.value = value;
        this.dimensionsJson = dimensionsJson;
        this.capturedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getMetricType() {
        return metricType;
    }

    public String getWindowType() {
        return windowType;
    }

    public long getWindowStart() {
        return windowStart;
    }

    public long getWindowEnd() {
        return windowEnd;
    }

    public double getValue() {
        return value;
    }

    public String getDimensionsJson() {
        return dimensionsJson;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }
}
