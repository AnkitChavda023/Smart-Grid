package com.smartgrid.contractservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sla_breaches")
public class SlaBreach {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vendor_id", nullable = false)
    private String vendorId;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(nullable = false)
    private String severity;

    @Column(name = "penalty_amount", nullable = false)
    private double penaltyAmount;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt = Instant.now();

    protected SlaBreach() {
    }

    public SlaBreach(String vendorId, UUID contractId, String severity, double penaltyAmount, String reason) {
        this.vendorId = vendorId;
        this.contractId = contractId;
        this.severity = severity;
        this.penaltyAmount = penaltyAmount;
        this.reason = reason;
    }

    public UUID getId() {
        return id;
    }

    public String getVendorId() {
        return vendorId;
    }

    public UUID getContractId() {
        return contractId;
    }

    public String getSeverity() {
        return severity;
    }

    public double getPenaltyAmount() {
        return penaltyAmount;
    }

    public String getReason() {
        return reason;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }
}
