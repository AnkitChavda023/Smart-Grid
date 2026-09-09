package com.smartgrid.forecasteragent.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Simple exponential smoothing: level_t = alpha*x_t + (1-alpha)*level_{t-1}, an O(1) update per
 * observed demand event. Daily quantities are bucketed from real reservation events first — the
 * smoothing itself never touches fabricated data, only the actually-observed order history.
 */
@Component
public class ExponentialSmoothingForecaster {

    public record Baseline(double dailyLevel, double dailyStdDev) {
    }

    public Baseline computeBaseline(List<DailyQuantity> dailyQuantities, double alpha) {
        if (dailyQuantities.isEmpty()) {
            return new Baseline(0.0, 0.0);
        }
        double level = dailyQuantities.get(0).quantity();
        for (int i = 1; i < dailyQuantities.size(); i++) {
            level = alpha * dailyQuantities.get(i).quantity() + (1 - alpha) * level;
        }

        double mean = dailyQuantities.stream().mapToDouble(DailyQuantity::quantity).average().orElse(0.0);
        double variance = dailyQuantities.stream()
                .mapToDouble(d -> Math.pow(d.quantity() - mean, 2))
                .average().orElse(0.0);
        return new Baseline(level, Math.sqrt(variance));
    }

    /** Buckets raw per-event quantities into whole-day totals so the smoothing operates on a real daily demand series. */
    public List<DailyQuantity> bucketByDay(List<RawEvent> events) {
        return events.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        e -> e.occurredAt().truncatedTo(ChronoUnit.DAYS),
                        java.util.TreeMap::new,
                        java.util.stream.Collectors.summingLong(RawEvent::quantity)))
                .entrySet().stream()
                .map(entry -> new DailyQuantity(entry.getKey(), entry.getValue()))
                .toList();
    }

    public record RawEvent(long quantity, Instant occurredAt) {
    }

    public record DailyQuantity(Instant day, long quantity) {
    }
}
