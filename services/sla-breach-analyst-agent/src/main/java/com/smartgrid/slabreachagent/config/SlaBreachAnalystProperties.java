package com.smartgrid.slabreachagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartgrid.sla-breach-analyst")
public record SlaBreachAnalystProperties(
        double confidenceThreshold,
        double breachProbabilityThreshold,
        int rollingWindowDays,
        int forecastHorizonDays,
        long targetStockLevel,
        ServiceEndpoint vendorService,
        ServiceEndpoint inventoryService,
        ServiceEndpoint ragService,
        McpServer mcpServer,
        OpenAi openai
) {
    public record ServiceEndpoint(String baseUrl) {
    }

    public record McpServer(String baseUrl, String internalToken) {
    }

    public record OpenAi(String apiKey, String baseUrl, String chatModel) {
    }
}
