package com.smartgrid.contractservice.dto;

import com.smartgrid.contractservice.domain.Contract;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ContractResponse(
        UUID id,
        String vendorId,
        String terms,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        List<SlaTermResponse> slaTerms
) {
    public record SlaTermResponse(String metricName, double thresholdValue, double penaltyPerBreach) {
    }

    public static ContractResponse from(Contract contract) {
        List<SlaTermResponse> terms = contract.getSlaTerms().stream()
                .map(t -> new SlaTermResponse(t.getMetricName(), t.getThresholdValue(), t.getPenaltyPerBreach()))
                .toList();
        return new ContractResponse(contract.getId(), contract.getVendorId(), contract.getTerms(),
                contract.getStartDate(), contract.getEndDate(), contract.isActive(), terms);
    }
}
