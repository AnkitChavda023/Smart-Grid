package com.smartgrid.disruptionagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartgrid.disruption")
public record DisruptionProperties(
        int windowMinutes,
        int minDistinctSignals,
        double confidenceThreshold,
        double scoreDropThreshold,
        ServiceEndpoint vendorService,
        ServiceEndpoint orderService,
        ServiceEndpoint ragService,
        OpenAi openai
) {
    public record ServiceEndpoint(String baseUrl) {
    }

    public record OpenAi(String apiKey, String baseUrl, String chatModel) {
    }
}
