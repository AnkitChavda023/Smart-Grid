package com.smartgrid.slabreachagent.client;

import com.smartgrid.slabreachagent.config.SlaBreachAnalystProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
public class VendorClient {

    private final RestClient restClient;

    public VendorClient(SlaBreachAnalystProperties properties) {
        this.restClient = RestClient.create(properties.vendorService().baseUrl());
    }

    public List<String> skuIdsFor(String vendorId) {
        VendorSku[] skus = restClient.get()
                .uri("/vendors/{id}/skus", UUID.fromString(vendorId))
                .retrieve()
                .body(VendorSku[].class);
        return skus == null ? List.of() : List.of(skus).stream().map(VendorSku::skuId).toList();
    }

    private record VendorSku(String skuId, double price, int leadTimeDays) {
    }
}
