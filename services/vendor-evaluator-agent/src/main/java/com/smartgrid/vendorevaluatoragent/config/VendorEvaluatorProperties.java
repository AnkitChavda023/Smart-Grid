package com.smartgrid.vendorevaluatoragent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartgrid.vendor-evaluator")
public record VendorEvaluatorProperties(
        double confidenceThreshold,
        int trendWindowDays,
        ServiceEndpoint vendorService,
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
