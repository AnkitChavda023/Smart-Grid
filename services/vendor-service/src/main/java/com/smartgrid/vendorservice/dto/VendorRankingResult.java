package com.smartgrid.vendorservice.dto;

import java.util.UUID;

public record VendorRankingResult(
        UUID vendorId,
        String vendorName,
        double compositeScore,
        double price,
        int leadTimeDays,
        double reliabilityScore
) {
}
