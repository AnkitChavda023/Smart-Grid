package com.smartgrid.contractservice.dto;

import com.smartgrid.contractservice.domain.ContractDraft;
import com.smartgrid.contractservice.domain.DraftStatus;

import java.time.Instant;
import java.util.UUID;

public record ContractDraftResponse(UUID id, String vendorId, UUID existingContractId, String proposedTerms,
                                     String summary, DraftStatus status, Instant createdAt) {

    public static ContractDraftResponse from(ContractDraft draft) {
        return new ContractDraftResponse(draft.getId(), draft.getVendorId(), draft.getExistingContractId(),
                draft.getProposedTerms(), draft.getSummary(), draft.getStatus(), draft.getCreatedAt());
    }
}
