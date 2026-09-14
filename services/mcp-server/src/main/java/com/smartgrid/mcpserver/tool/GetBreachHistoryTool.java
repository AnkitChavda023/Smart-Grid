package com.smartgrid.mcpserver.tool;

import com.smartgrid.mcpserver.config.DownstreamClients;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetBreachHistoryTool implements McpTool {

    private final DownstreamClients clients;

    public GetBreachHistoryTool(DownstreamClients clients) {
        this.clients = clients;
    }

    @Override
    public String name() {
        return "getBreachHistory";
    }

    @Override
    public int version() {
        return 1;
    }

    @Override
    public String description() {
        return "Returns a vendor's SLA breach history.";
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
                        "id", new ToolSchema.PropertySchema("string", "Breach id"),
                        "contractId", new ToolSchema.PropertySchema("string", "Contract id"),
                        "severity", new ToolSchema.PropertySchema("string", "Breach severity"),
                        "penaltyAmount", new ToolSchema.PropertySchema("number", "Penalty amount"),
                        "reason", new ToolSchema.PropertySchema("string", "Breach reason"),
                        "detectedAt", new ToolSchema.PropertySchema("string", "Detection timestamp")),
                List.of());
    }

    @Override
    public Object invoke(Map<String, Object> params) {
        String vendorId = ToolParams.requireString(params, "vendorId");

        SlaBreachDto[] breaches = clients.contractService().get()
                .uri("/sla-breaches?vendorId={vendorId}", vendorId)
                .retrieve()
                .body(SlaBreachDto[].class);

        return breaches == null ? List.of() : List.of(breaches);
    }

    private record SlaBreachDto(UUID id, String vendorId, UUID contractId, String severity,
                                 double penaltyAmount, String reason, Instant detectedAt) {
    }
}
