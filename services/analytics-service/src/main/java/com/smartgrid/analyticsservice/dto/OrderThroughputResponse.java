package com.smartgrid.analyticsservice.dto;

public record OrderThroughputResponse(String stage, String region, long windowStart, long windowEnd, double count) {
}
