package com.smartgrid.vendorservice.dto;

import com.smartgrid.vendorservice.domain.VendorHealthReport;

import java.time.Instant;
import java.util.UUID;

public record VendorHealthReportResponse(UUID id, UUID vendorId, String trendDirection, double trendSlope,
                                          double averageLeadTimeDays, int breachCount, String summary, Instant createdAt) {

    public static VendorHealthReportResponse from(VendorHealthReport report) {
        return new VendorHealthReportResponse(report.getId(), report.getVendorId(), report.getTrendDirection(),
                report.getTrendSlope(), report.getAverageLeadTimeDays(), report.getBreachCount(),
                report.getSummary(), report.getCreatedAt());
    }
}
