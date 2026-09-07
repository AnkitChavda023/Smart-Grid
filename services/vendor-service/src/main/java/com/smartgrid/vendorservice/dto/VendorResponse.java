package com.smartgrid.vendorservice.dto;

import com.smartgrid.vendorservice.domain.Vendor;

import java.util.List;
import java.util.UUID;

public record VendorResponse(
        UUID id,
        String name,
        String region,
        Double latitude,
        Double longitude,
        String capabilities,
        String contact,
        String category,
        String certifications,
        boolean suspended,
        double reliabilityScore,
        List<VendorSkuResponse> skus,
        String scoreFormula
) {
    public static VendorResponse from(Vendor vendor, double reliabilityScore) {
        List<VendorSkuResponse> skuList = vendor.getSkus() != null
                ? vendor.getSkus().stream().map(VendorSkuResponse::from).toList()
                : List.of();
        return new VendorResponse(
                vendor.getId(), vendor.getName(), vendor.getRegion(),
                vendor.getLatitude(), vendor.getLongitude(), vendor.getCapabilities(),
                vendor.getContact(), vendor.getCategory(), vendor.getCertifications(),
                vendor.isSuspended(), reliabilityScore, skuList,
                "Price (40%) + Lead Time (35%) + Reliability (25%)"
        );
    }
}
