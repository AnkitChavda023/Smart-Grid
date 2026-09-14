package com.smartgrid.vendorservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vendor_scores")
public class VendorScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vendor_id", nullable = false, unique = true)
    private UUID vendorId;

    @Column(name = "reliability_score", nullable = false)
    private double reliabilityScore;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected VendorScore() {
    }

    public VendorScore(UUID vendorId, double reliabilityScore) {
        this.vendorId = vendorId;
        this.reliabilityScore = reliabilityScore;
    }

    public UUID getVendorId() {
        return vendorId;
    }

    public double getReliabilityScore() {
        return reliabilityScore;
    }

    public void setReliabilityScore(double reliabilityScore) {
        this.reliabilityScore = reliabilityScore;
        this.updatedAt = Instant.now();
    }
}
