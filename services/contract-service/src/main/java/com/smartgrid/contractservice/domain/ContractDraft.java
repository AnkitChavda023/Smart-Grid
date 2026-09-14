package com.smartgrid.contractservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a contract draft.
 */
@Entity
@Table(name = "contract_drafts")
public class ContractDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vendor_id", nullable = false)
    private String vendorId;

    @Column(name = "existing_contract_id")
    private UUID existingContractId;

    @Column(name = "proposed_terms", nullable = false, columnDefinition = "text")
    private String proposedTerms;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DraftStatus status = DraftStatus.DRAFT;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ContractDraft() {
    }

    public ContractDraft(String vendorId, UUID existingContractId, String proposedTerms, String summary) {
        this.vendorId = vendorId;
        this.existingContractId = existingContractId;
        this.proposedTerms = proposedTerms;
        this.summary = summary;
    }

    public void markSubmitted() {
        this.status = DraftStatus.SUBMITTED;
    }

    public void markRejected() {
        this.status = DraftStatus.REJECTED;
    }

    public void updateProposedTerms(String proposedTerms, String summary) {
        if (proposedTerms != null && !proposedTerms.isBlank()) {
            this.proposedTerms = proposedTerms;
        }
        if (summary != null && !summary.isBlank()) {
            this.summary = summary;
        }
    }

    public UUID getId() {
        return id;
    }

    public String getVendorId() {
        return vendorId;
    }

    public UUID getExistingContractId() {
        return existingContractId;
    }

    public String getProposedTerms() {
        return proposedTerms;
    }

    public String getSummary() {
        return summary;
    }

    public DraftStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
