package com.smartgrid.mcpserver.tool;

import com.smartgrid.mcpserver.config.DownstreamClients;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * No external market-data feed exists anywhere in this system — this calls contract-service's
 * {@code GET /contracts/benchmarks}, which computes aggregate statistics across every real SLA
 * term on file for the given metric, not fabricated external data.
 */
@Component
public class GetMarketBenchmarksTool implements McpTool {

    private final DownstreamClients clients;

    public GetMarketBenchmarksTool(DownstreamClients clients) {
        this.clients = clients;
    }

    @Override
    public String name() {
        return "getMarketBenchmarks";
    }

    @Override
    public int version() {
        return 1;
    }

    @Override
    public String description() {
        return "Returns average threshold and penalty values across all real contracts currently on file for an SLA metric.";
    }

    @Override
    public ToolSchema inputSchema() {
        return ToolSchema.object(Map.of(
                        "metricName", new ToolSchema.PropertySchema("string", "SLA metric name, e.g. on_time_delivery_rate")),
                List.of("metricName"));
    }

    @Override
    public ToolSchema outputSchema() {
        return ToolSchema.object(Map.of(
                        "metricName", new ToolSchema.PropertySchema("string", "SLA metric name"),
                        "averageThreshold", new ToolSchema.PropertySchema("number", "Average threshold across all contracts with this metric"),
                        "averagePenalty", new ToolSchema.PropertySchema("number", "Average penalty per breach across all contracts with this metric"),
                        "sampleSize", new ToolSchema.PropertySchema("integer", "Number of contracts the average is computed over")),
                List.of());
    }

    @Override
    public Object invoke(Map<String, Object> params) {
        String metricName = ToolParams.requireString(params, "metricName");

        return clients.contractService().get()
                .uri("/contracts/benchmarks?metricName={metricName}", metricName)
                .retrieve()
                .body(Object.class);
    }
}
