package com.smartgrid.vendorservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateVendorRequest(
        @NotBlank String name,
        @NotBlank String region,
        Double latitude,
        Double longitude,
        String capabilities,
        String contact,
        String category,
        String certifications,
        @Valid List<VendorSkuRequest> skus
) {
    public CreateVendorRequest(String name, String region, Double latitude, Double longitude, String capabilities, List<VendorSkuRequest> skus) {
        this(name, region, latitude, longitude, capabilities, null, null, null, skus);
    }
}
