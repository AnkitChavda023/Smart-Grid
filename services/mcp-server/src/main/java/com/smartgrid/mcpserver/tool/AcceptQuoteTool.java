package com.smartgrid.mcpserver.tool;

import com.smartgrid.mcpserver.config.DownstreamClients;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AcceptQuoteTool implements McpTool {

    private final DownstreamClients clients;

    public AcceptQuoteTool(DownstreamClients clients) {
        this.clients = clients;
    }

    @Override
    public String name() {
        return "acceptQuote";
    }

    @Override
    public int version() {
        return 1;
    }

    @Override
    public String description() {
        return "Accepts a previously created quote.";
    }

    @Override
    public ToolSchema inputSchema() {
        return ToolSchema.object(Map.of(
                        "quoteId", new ToolSchema.PropertySchema("string", "Quote id to accept")),
                List.of("quoteId"));
    }

    @Override
    public ToolSchema outputSchema() {
        return ToolSchema.object(Map.of(
                        "quoteId", new ToolSchema.PropertySchema("string", "Accepted quote id"),
                        "accepted", new ToolSchema.PropertySchema("boolean", "Whether the accept call succeeded")),
                List.of());
    }

    @Override
    public Object invoke(Map<String, Object> params) {
        String quoteId = ToolParams.requireString(params, "quoteId");

        clients.pricingService().post()
                .uri("/quotes/{id}/accept", quoteId)
                .retrieve()
                .toBodilessEntity();

        return new AcceptResult(quoteId, true);
    }

    private record AcceptResult(String quoteId, boolean accepted) {
    }
}
