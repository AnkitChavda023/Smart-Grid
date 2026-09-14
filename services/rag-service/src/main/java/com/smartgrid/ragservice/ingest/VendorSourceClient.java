package com.smartgrid.ragservice.ingest;

import com.smartgrid.ragservice.config.RagProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@Component
public class VendorSourceClient {

    private final RestClient restClient;

    public VendorSourceClient(RagProperties properties) {
        this.restClient = RestClient.create(properties.vendorService().baseUrl());
    }

    /** {@code /vendors/search} with no query returns the full catalog — the search index's document shape, not VendorResponse. */
    public List<VendorCatalogEntry> listAll() {
        VendorCatalogEntry[] vendors = restClient.get()
                .uri("/vendors/search")
                .retrieve()
                .body(VendorCatalogEntry[].class);
        return vendors == null ? List.of() : List.of(vendors);
    }

    public VendorCatalogEntry getById(UUID id) {
        VendorDetail detail = restClient.get()
                .uri("/vendors/{id}", id)
                .retrieve()
                .body(VendorDetail.class);
        return detail == null ? null : new VendorCatalogEntry(detail.id().toString(), detail.name(), detail.region(), detail.capabilities());
    }

    public record VendorCatalogEntry(String id, String name, String region, String capabilities) {
    }

    private record VendorDetail(UUID id, String name, String region, Double latitude, Double longitude,
                                 String capabilities, boolean suspended, double reliabilityScore) {
    }
}
