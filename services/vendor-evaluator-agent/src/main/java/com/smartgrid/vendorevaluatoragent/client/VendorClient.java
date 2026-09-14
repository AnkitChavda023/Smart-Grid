package com.smartgrid.vendorevaluatoragent.client;

import com.smartgrid.vendorevaluatoragent.config.VendorEvaluatorProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
public class VendorClient {

    private final RestClient restClient;

    public VendorClient(VendorEvaluatorProperties properties) {
        this.restClient = RestClient.create(properties.vendorService().baseUrl());
    }

    public double currentAverageLeadTimeDays(String vendorId) {
        VendorSku[] skus = restClient.get()
                .uri("/vendors/{id}/skus", UUID.fromString(vendorId))
                .retrieve()
                .body(VendorSku[].class);
        List<VendorSku> list = skus == null ? List.of() : List.of(skus);
        return list.stream().mapToInt(VendorSku::leadTimeDays).average().orElse(0);
    }

    private record VendorSku(String skuId, double price, int leadTimeDays) {
    }
}
