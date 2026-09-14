package com.smartgrid.rerouteagent.client;

import com.smartgrid.rerouteagent.config.RerouteProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class OrderClient {

    private final RestClient restClient;

    public OrderClient(RerouteProperties properties) {
        this.restClient = RestClient.create(properties.orderService().baseUrl());
    }

    public List<AffectedOrder> findReroutableOrders(String region, List<String> skus) {
        AffectedOrder[] orders = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/orders/affected")
                        .queryParam("region", region)
                        .queryParam("skus", skus)
                        .build())
                .retrieve()
                .body(AffectedOrder[].class);
        return orders == null ? List.of() : List.of(orders);
    }

    public record AffectedOrder(String id, String status, long version, String requestedBy,
                                 String destinationRegion, List<Object> items, String vendorId, String quoteId) {
    }
}
