package com.smartgrid.shipmentservice.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class EtaCalculationService {

    private static final double DELAY_THRESHOLD_MULTIPLIER = 1.5;

    public record EtaResult(Double etaMinutes, boolean delayed, Double latestGapMinutes, Double baselineAvgMinutes) {
    }

    // Bounded to at most the last 5 intervals (per the caller), so this is O(1) work regardless of shipment age.
    public EtaResult computeEta(List<Instant> checkpointsChronological, int checkpointsRemaining) {
        if (checkpointsChronological.size() < 2) {
            return new EtaResult(null, false, null, null);
        }

        List<Double> intervals = new java.util.ArrayList<>();
        for (int i = 1; i < checkpointsChronological.size(); i++) {
            intervals.add(minutesBetween(checkpointsChronological.get(i - 1), checkpointsChronological.get(i)));
        }

        double latestGap = intervals.get(intervals.size() - 1);
        List<Double> priorIntervals = intervals.subList(0, intervals.size() - 1);

        double baselineAvg = priorIntervals.isEmpty() ? latestGap : weightedAverage(priorIntervals);
        boolean delayed = !priorIntervals.isEmpty() && latestGap > baselineAvg * DELAY_THRESHOLD_MULTIPLIER;

        double overallAvg = weightedAverage(intervals);
        double etaMinutes = overallAvg * Math.max(checkpointsRemaining, 0);

        return new EtaResult(etaMinutes, delayed, latestGap, baselineAvg);
    }

    // Weights 1..n, oldest to newest - each interval counts for more than the one before it.
    private double weightedAverage(List<Double> values) {
        double weightedSum = 0;
        double weightTotal = 0;
        for (int i = 0; i < values.size(); i++) {
            double weight = i + 1;
            weightedSum += values.get(i) * weight;
            weightTotal += weight;
        }
        return weightedSum / weightTotal;
    }

    private double minutesBetween(Instant a, Instant b) {
        return Duration.between(a, b).toMillis() / 60000.0;
    }
}
