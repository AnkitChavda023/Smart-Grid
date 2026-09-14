package com.smartgrid.vendorevaluatoragent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity tracking historical vendor lead-time observations for trend regression analysis.
 */
@Entity
@Table(name = "vendor_lead_time_observations")
public class VendorLeadTimeObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vendor_id", nullable = false)
    private String vendorId;

    @Column(name = "average_lead_time_days", nullable = false)
    private double averageLeadTimeDays;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt = Instant.now();

    protected VendorLeadTimeObservation() {
    }

    public VendorLeadTimeObservation(String vendorId, double averageLeadTimeDays) {
        this.vendorId = vendorId;
        this.averageLeadTimeDays = averageLeadTimeDays;
    }

    public UUID getId() {
        return id;
    }

    public String getVendorId() {
        return vendorId;
    }

    public double getAverageLeadTimeDays() {
        return averageLeadTimeDays;
    }

    public Instant getObservedAt() {
        return observedAt;
    }
}
