package com.smartgrid.ragservice.ingest;

import com.smartgrid.ragservice.config.RagProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class ContractSourceClient {

    private final RestClient restClient;

    public ContractSourceClient(RagProperties properties) {
        this.restClient = RestClient.create(properties.contractService().baseUrl());
    }

    public List<ContractRecord> listAll() {
        ContractRecord[] contracts = restClient.get()
                .uri("/contracts")
                .retrieve()
                .body(ContractRecord[].class);
        return contracts == null ? List.of() : List.of(contracts);
    }

    public List<ContractRecord> activeForVendor(String vendorId) {
        ContractRecord[] contracts = restClient.get()
                .uri("/contracts/{vendorId}/active", vendorId)
                .retrieve()
                .body(ContractRecord[].class);
        return contracts == null ? List.of() : List.of(contracts);
    }

    public List<SlaBreachRecord> breachesForVendor(String vendorId) {
        SlaBreachRecord[] breaches = restClient.get()
                .uri("/sla-breaches?vendorId={vendorId}", vendorId)
                .retrieve()
                .body(SlaBreachRecord[].class);
        return breaches == null ? List.of() : List.of(breaches);
    }

    public record ContractRecord(UUID id, String vendorId, String terms, LocalDate startDate, LocalDate endDate,
                                  boolean active, List<Object> slaTerms) {
    }

    public record SlaBreachRecord(UUID id, String vendorId, UUID contractId, String severity,
                                   double penaltyAmount, String reason, Instant detectedAt) {
    }
}
