package com.smartgrid.mcpserver.tool;

import com.smartgrid.mcpserver.config.DownstreamClients;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * There is no dedicated "risk profile" store anywhere in the system — this composes two real,
 * already-existing data sources (vendor-service's reliability score, contract-service's real SLA
 * breach history) into a computed view, rather than a new persisted entity nothing else populates.
 */
@Component
public class GetVendorRiskProfileTool implements McpTool {

    private final DownstreamClients clients;

    public GetVendorRiskProfileTool(DownstreamClients clients) {
        this.clients = clients;
    }

    @Override
    public String name() {
        return "getVendorRiskProfile";
    }

    @Override
    public int version() {
        return 1;
    }

    @Override
    public String description() {
        return "Computes a vendor risk profile from real data: current reliability score plus SLA breach frequency and severity mix.";
    }

    @Override
    public ToolSchema inputSchema() {
        return ToolSchema.object(Map.of(
                        "vendorId", new ToolSchema.PropertySchema("string", "Vendor id")),
                List.of("vendorId"));
    }

    @Override
    public ToolSchema outputSchema() {
        return ToolSchema.object(Map.of(
                        "vendorId", new ToolSchema.PropertySchema("string", "Vendor id"),
                        "reliabilityScore", new ToolSchema.PropertySchema("number", "Current reliability score"),
                        "breachCount", new ToolSchema.PropertySchema("integer", "Total SLA breaches on file"),
                        "criticalBreachCount", new ToolSchema.PropertySchema("integer", "Breaches with severity CRITICAL or HIGH"),
                        "totalPenaltyAmount", new ToolSchema.PropertySchema("number", "Sum of penalty amounts across all breaches")),
                List.of());
    }

    @Override
    public Object invoke(Map<String, Object> params) {
        String vendorId = ToolParams.requireString(params, "vendorId");

        VendorDetail vendor = clients.vendorService().get()
                .uri("/vendors/{id}", UUID.fromString(vendorId))
                .retrieve()
                .body(VendorDetail.class);

        SlaBreachDto[] breaches = clients.contractService().get()
                .uri("/sla-breaches?vendorId={vendorId}", vendorId)
                .retrieve()
                .body(SlaBreachDto[].class);
        List<SlaBreachDto> breachList = breaches == null ? List.of() : List.of(breaches);

        long criticalCount = breachList.stream()
                .filter(b -> "CRITICAL".equalsIgnoreCase(b.severity()) || "HIGH".equalsIgnoreCase(b.severity()))
                .count();
        double totalPenalty = breachList.stream().mapToDouble(SlaBreachDto::penaltyAmount).sum();

        return new RiskProfile(vendorId, vendor == null ? 0.0 : vendor.reliabilityScore(),
                breachList.size(), (int) criticalCount, totalPenalty);
    }

    private record VendorDetail(UUID id, String name, String region, Double latitude, Double longitude,
                                 String capabilities, boolean suspended, double reliabilityScore) {
    }

    private record SlaBreachDto(UUID id, String vendorId, UUID contractId, String severity,
                                 double penaltyAmount, String reason, Instant detectedAt) {
    }

    private record RiskProfile(String vendorId, double reliabilityScore, int breachCount,
                                int criticalBreachCount, double totalPenaltyAmount) {
    }
}
