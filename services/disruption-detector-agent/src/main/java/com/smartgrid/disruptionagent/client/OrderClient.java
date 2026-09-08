package com.smartgrid.disruptionagent.client;

import com.smartgrid.disruptionagent.config.DisruptionProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.UUID;

@Component
public class OrderClient {

    private final RestClient restClient;

    public OrderClient(DisruptionProperties properties) {
        this.restClient = RestClient.create(properties.orderService().baseUrl());
    }

    /** Resolves the vendor a shipment delay should be attributed to, if the order is still known. */
    public Optional<String> getVendorId(String orderId) {
        try {
            OrderSummary order = restClient.get()
                    .uri("/orders/{id}", UUID.fromString(orderId))
                    .retrieve()
                    .body(OrderSummary.class);
            return Optional.ofNullable(order).map(OrderSummary::vendorId).filter(v -> v != null && !v.isBlank());
        } catch (IllegalArgumentException | RestClientException e) {
            return Optional.empty();
        }
    }

    private record OrderSummary(UUID id, String vendorId) {
    }
}
