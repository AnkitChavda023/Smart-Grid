package com.smartgrid.rerouteagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartgrid.reroute")
public record RerouteProperties(
        int maxToolCalls,
        double confidenceThreshold,
        ServiceEndpoint orderService,
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
