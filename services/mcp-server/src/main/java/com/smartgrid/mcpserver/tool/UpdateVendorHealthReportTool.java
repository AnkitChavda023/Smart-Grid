package com.smartgrid.mcpserver.tool;

import com.smartgrid.mcpserver.config.DownstreamClients;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class UpdateVendorHealthReportTool implements McpTool {

    private final DownstreamClients clients;

    public UpdateVendorHealthReportTool(DownstreamClients clients) {
        this.clients = clients;
    }

    @Override
    public String name() {
        return "updateVendorHealthReport";
    }

    @Override
    public int version() {
        return 1;
    }

    @Override
    public String description() {
        return "Persists a vendor health report (trend direction/slope, average lead time, breach count, summary) computed by the Vendor Evaluator agent.";
    }

    @Override
    public ToolSchema inputSchema() {
        return ToolSchema.object(Map.of(
                        "vendorId", new ToolSchema.PropertySchema("string", "Vendor id"),
                        "trendDirection", new ToolSchema.PropertySchema("string", "IMPROVING, DECLINING, or STABLE"),
                        "trendSlope", new ToolSchema.PropertySchema("number", "Linear regression slope over observed lead times"),
                        "averageLeadTimeDays", new ToolSchema.PropertySchema("number", "Average lead time in days"),
                        "breachCount", new ToolSchema.PropertySchema("integer", "SLA breach count considered"),
                        "summary", new ToolSchema.PropertySchema("string", "Human-readable summary of the report")),
                List.of("vendorId", "trendDirection", "summary"));
    }

    @Override
    public ToolSchema outputSchema() {
        return ToolSchema.object(Map.of(
                        "id", new ToolSchema.PropertySchema("string", "Created health report id"),
                        "vendorId", new ToolSchema.PropertySchema("string", "Vendor id")),
                List.of());
    }

    @Override
    public Object invoke(Map<String, Object> params) {
        String vendorId = ToolParams.requireString(params, "vendorId");
        Map<String, Object> body = Map.of(
                "trendDirection", ToolParams.requireString(params, "trendDirection"),
                "trendSlope", ToolParams.optionalDouble(params, "trendSlope", 0.0),
                "averageLeadTimeDays", ToolParams.optionalDouble(params, "averageLeadTimeDays", 0.0),
                "breachCount", ToolParams.optionalInt(params, "breachCount", 0),
                "summary", ToolParams.requireString(params, "summary"));

        return clients.vendorService().post()
                .uri("/vendors/{id}/health-reports", UUID.fromString(vendorId))
                .body(body)
                .retrieve()
                .body(Object.class);
    }
}
