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
@Table(name = "penalties")
public class Penalty {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "vendor_id", nullable = false)
    private String vendorId;

    @Column(nullable = false)
    private double amount;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt = Instant.now();

    protected Penalty() {
    }

    public Penalty(UUID contractId, String vendorId, double amount) {
        this.contractId = contractId;
        this.vendorId = vendorId;
        this.amount = amount;
    }

    public UUID getId() {
        return id;
    }

    public UUID getContractId() {
        return contractId;
    }

    public String getVendorId() {
        return vendorId;
    }

    public double getAmount() {
        return amount;
    }

    public Instant getComputedAt() {
        return computedAt;
    }
}
