package com.smartgrid.contractservice.dto;

import com.smartgrid.contractservice.domain.SlaBreach;

import java.time.Instant;
import java.util.UUID;

public record SlaBreachResponse(UUID id, String vendorId, UUID contractId, String severity, double penaltyAmount, String reason, Instant detectedAt) {

    public static SlaBreachResponse from(SlaBreach breach) {
        return new SlaBreachResponse(breach.getId(), breach.getVendorId(), breach.getContractId(),
                breach.getSeverity(), breach.getPenaltyAmount(), breach.getReason(), breach.getDetectedAt());
    }
}
