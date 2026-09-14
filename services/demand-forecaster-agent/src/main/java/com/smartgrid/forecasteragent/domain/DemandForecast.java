package com.smartgrid.forecasteragent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "demand_forecasts")
public class DemandForecast {

    @Id
    private UUID id;

    @Column(name = "sku_id", nullable = false)
    private String skuId;

    @Column(name = "horizon_days", nullable = false)
    private int horizonDays;

    @Column(nullable = false)
    private double p10;

    @Column(nullable = false)
    private double p50;

    @Column(nullable = false)
    private double p90;

    @Column(nullable = false)
    private double confidence;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected DemandForecast() {
    }

    public DemandForecast(UUID id, String skuId, int horizonDays, double p10, double p50, double p90,
                           double confidence, String summary) {
        this.id = id;
        this.skuId = skuId;
        this.horizonDays = horizonDays;
        this.p10 = p10;
        this.p50 = p50;
        this.p90 = p90;
        this.confidence = confidence;
        this.summary = summary;
    }

    public UUID getId() {
        return id;
    }

    public String getSkuId() {
        return skuId;
    }

    public int getHorizonDays() {
        return horizonDays;
    }

    public double getP10() {
        return p10;
    }

    public double getP50() {
        return p50;
    }

    public double getP90() {
        return p90;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
