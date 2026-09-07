package com.smartgrid.mcpserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartgrid.mcp")
public record McpProperties(
        String internalToken,
        ServiceEndpoint vendorService,
        ServiceEndpoint inventoryService,
        ServiceEndpoint pricingService,
        ServiceEndpoint analyticsService,
        ServiceEndpoint contractService,
        ServiceEndpoint notificationService
) {
    public record ServiceEndpoint(String baseUrl) {
    }
}
