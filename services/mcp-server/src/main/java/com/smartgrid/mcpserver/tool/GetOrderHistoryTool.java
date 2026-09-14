package com.smartgrid.mcpserver.tool;

import com.smartgrid.mcpserver.config.DownstreamClients;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * analytics-service's {@code /analytics/orders/throughput} has no SKU dimension at all (only
 * stage/region/window) — the service just doesn't track that breakdown. {@code skuId} and
 * {@code lookbackDays} are accepted for schema fidelity but cannot be applied to the real data.
 */
@Component
public class GetOrderHistoryTool implements McpTool {

    private final DownstreamClients clients;

    public GetOrderHistoryTool(DownstreamClients clients) {
        this.clients = clients;
    }

    @Override
    public String name() {
        return "getOrderHistory";
    }

    @Override
    public int version() {
        return 1;
    }

    @Override
    public String description() {
        return "Returns order throughput by stage and region, computed from the analytics service's Kafka Streams windows.";
    }

    @Override
    public ToolSchema inputSchema() {
        return ToolSchema.object(Map.of(
                        "skuId", new ToolSchema.PropertySchema("string", "Not applied — throughput has no SKU dimension in this system"),
                        "lookbackDays", new ToolSchema.PropertySchema("integer", "Not applied — throughput windows are fixed, not lookback-configurable")),
                List.of());
    }

    @Override
    public ToolSchema outputSchema() {
        return ToolSchema.object(Map.of(
                        "stage", new ToolSchema.PropertySchema("string", "Order lifecycle stage"),
                        "region", new ToolSchema.PropertySchema("string", "Region"),
                        "windowStart", new ToolSchema.PropertySchema("integer", "Window start epoch millis"),
                        "windowEnd", new ToolSchema.PropertySchema("integer", "Window end epoch millis"),
                        "count", new ToolSchema.PropertySchema("number", "Order count in window")),
                List.of());
    }

    @Override
    public Object invoke(Map<String, Object> params) {
        OrderThroughputDto[] result = clients.analyticsService().get()
                .uri("/analytics/orders/throughput")
                .retrieve()
                .body(OrderThroughputDto[].class);
        return result == null ? List.of() : List.of(result);
    }

    private record OrderThroughputDto(String stage, String region, long windowStart, long windowEnd, double count) {
    }
}
