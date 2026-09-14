package com.smartgrid.vendorservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record VendorHealthReportRequest(
        @NotBlank String trendDirection,
        double trendSlope,
        @PositiveOrZero double averageLeadTimeDays,
        @PositiveOrZero int breachCount,
        @NotBlank String summary
) {
}
