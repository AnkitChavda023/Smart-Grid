package com.smartgrid.contractservice.dto;

public record BenchmarkResponse(String metricName, Double averageThreshold, Double averagePenalty, long sampleSize) {
}
