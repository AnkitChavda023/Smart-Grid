package com.smartgrid.forecasteragent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartgrid.demand-forecaster")
public record DemandForecasterProperties(
        double confidenceThreshold,
        double smoothingAlpha,
        int reorderLookbackDays,
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
