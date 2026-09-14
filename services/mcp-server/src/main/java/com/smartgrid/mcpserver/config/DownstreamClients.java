package com.smartgrid.mcpserver.config;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class DownstreamClients {

    private final RestClient vendorService;
    private final RestClient inventoryService;
    private final RestClient pricingService;
    private final RestClient analyticsService;
    private final RestClient contractService;
    private final RestClient notificationService;

    public DownstreamClients(McpProperties properties) {
        this.vendorService = RestClient.create(properties.vendorService().baseUrl());
        this.inventoryService = RestClient.create(properties.inventoryService().baseUrl());
        this.pricingService = RestClient.create(properties.pricingService().baseUrl());
        this.analyticsService = RestClient.create(properties.analyticsService().baseUrl());
        this.contractService = RestClient.create(properties.contractService().baseUrl());
        this.notificationService = RestClient.create(properties.notificationService().baseUrl());
    }

    public RestClient vendorService() {
        return vendorService;
    }

    public RestClient inventoryService() {
        return inventoryService;
    }

    public RestClient pricingService() {
        return pricingService;
    }

    public RestClient analyticsService() {
        return analyticsService;
    }

    public RestClient contractService() {
        return contractService;
    }

    public RestClient notificationService() {
        return notificationService;
    }
}
