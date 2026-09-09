package com.smartgrid.contractagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartgrid.contract-negotiation")
public record ContractNegotiationProperties(
        double confidenceThreshold,
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
