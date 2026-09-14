package com.smartgrid.analyticsservice.dto;

public record DisruptionSummaryResponse(String region, String windowType, long windowStart, long windowEnd, double count) {
}
