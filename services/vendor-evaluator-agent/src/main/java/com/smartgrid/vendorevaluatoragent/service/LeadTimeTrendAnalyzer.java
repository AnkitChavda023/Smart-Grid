package com.smartgrid.vendorevaluatoragent.service;

import com.smartgrid.vendorevaluatoragent.domain.VendorLeadTimeObservation;
import org.springframework.stereotype.Component;

import java.util.List;

/** Ordinary least-squares slope over (dayOffset, leadTimeDays) pairs — O(n) per evaluation, per the module spec. */
@Component
public class LeadTimeTrendAnalyzer {

    private static final double FLAT_SLOPE_THRESHOLD = 0.01;

    public TrendResult analyze(List<VendorLeadTimeObservation> observations) {
        int n = observations.size();
        if (n < 2) {
            return new TrendResult("STABLE", 0.0);
        }

        long baseEpoch = observations.get(0).getObservedAt().getEpochSecond();
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (VendorLeadTimeObservation obs : observations) {
            double x = (obs.getObservedAt().getEpochSecond() - baseEpoch) / 86400.0; // days since first observation
            double y = obs.getAverageLeadTimeDays();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }

        double denominator = (n * sumXX) - (sumX * sumX);
        double slope = denominator == 0 ? 0.0 : ((n * sumXY) - (sumX * sumY)) / denominator;

        String direction = Math.abs(slope) < FLAT_SLOPE_THRESHOLD ? "STABLE" : (slope > 0 ? "DECLINING" : "IMPROVING");
        return new TrendResult(direction, slope);
    }

    public record TrendResult(String direction, double slope) {
    }
}
