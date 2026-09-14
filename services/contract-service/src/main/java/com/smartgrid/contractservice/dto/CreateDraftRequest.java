package com.smartgrid.contractservice.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateDraftRequest(@NotBlank String vendorId, UUID existingContractId, @NotBlank String proposedTerms, @NotBlank String summary) {
}
