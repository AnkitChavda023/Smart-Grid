package com.smartgrid.disruptionagent.client;

import com.smartgrid.disruptionagent.config.DisruptionProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
public class VendorClient {

    private final RestClient restClient;

    public VendorClient(DisruptionProperties properties) {
        this.restClient = RestClient.create(properties.vendorService().baseUrl());
    }

    public VendorDetail getVendor(String vendorId) {
        return restClient.get()
                .uri("/vendors/{id}", UUID.fromString(vendorId))
                .retrieve()
                .body(VendorDetail.class);
    }

    public List<String> getSkuIds(String vendorId) {
        VendorSku[] skus = restClient.get()
                .uri("/vendors/{id}/skus", UUID.fromString(vendorId))
                .retrieve()
                .body(VendorSku[].class);
        return skus == null ? List.of() : java.util.Arrays.stream(skus).map(VendorSku::skuId).toList();
    }

    public record VendorDetail(UUID id, String name, String region, Double latitude, Double longitude,
                                String capabilities, boolean suspended, double reliabilityScore) {
    }

    public record VendorSku(String skuId, double price, int leadTimeDays) {
    }
}
