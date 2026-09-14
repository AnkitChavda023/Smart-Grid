package com.smartgrid.analyticsservice.dto;

public record VendorPerformanceResponse(String vendorId, Double slaBreachCount, Double latestScore) {
}
