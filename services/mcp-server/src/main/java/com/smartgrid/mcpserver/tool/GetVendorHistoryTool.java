package com.smartgrid.mcpserver.tool;

import com.smartgrid.mcpserver.config.DownstreamClients;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * analytics-service's {@code /analytics/vendors/performance} accepts a {@code period} string
 * (default "7d") but has no vendor-scoped filter — it always returns every vendor. This tool maps
 * {@code lookbackDays} onto {@code period} best-effort and filters the result down to the requested
 * vendorId client-side, since the server won't do it.
 */
@Component
public class GetVendorHistoryTool implements McpTool {

    private final DownstreamClients clients;

    public GetVendorHistoryTool(DownstreamClients clients) {
        this.clients = clients;
    }

    @Override
    public String name() {
        return "getVendorHistory";
    }

    @Override
    public int version() {
        return 1;
    }

    @Override
    public String description() {
        return "Returns a vendor's SLA breach count and latest performance score over a lookback window.";
    }

    @Override
    public ToolSchema inputSchema() {
        return ToolSchema.object(Map.of(
                        "vendorId", new ToolSchema.PropertySchema("string", "Vendor id"),
                        "lookbackDays", new ToolSchema.PropertySchema("integer", "Mapped to analytics-service's period param, e.g. 7 -> \"7d\"")),
                List.of("vendorId"));
    }

    @Override
    public ToolSchema outputSchema() {
        return ToolSchema.object(Map.of(
                        "vendorId", new ToolSchema.PropertySchema("string", "Vendor id"),
                        "slaBreachCount", new ToolSchema.PropertySchema("number", "SLA breaches in the window"),
                        "latestScore", new ToolSchema.PropertySchema("number", "Latest vendor performance score")),
                List.of());
    }

    @Override
    public Object invoke(Map<String, Object> params) {
        String vendorId = ToolParams.requireString(params, "vendorId");
        int lookbackDays = ToolParams.optionalInt(params, "lookbackDays", 7);

        VendorPerformanceDto[] all = clients.analyticsService().get()
                .uri("/analytics/vendors/performance?period={period}", lookbackDays + "d")
                .retrieve()
                .body(VendorPerformanceDto[].class);

        return (all == null ? List.<VendorPerformanceDto>of() : List.of(all)).stream()
                .filter(v -> vendorId.equals(v.vendorId()))
                .findFirst()
                .orElse(new VendorPerformanceDto(vendorId, 0.0, null));
    }

    private record VendorPerformanceDto(String vendorId, Double slaBreachCount, Double latestScore) {
    }
}
