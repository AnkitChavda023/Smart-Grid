package com.smartgrid.forecasteragent.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ExponentialSmoothingForecasterTest {

    private final ExponentialSmoothingForecaster forecaster = new ExponentialSmoothingForecaster();

    @Test
    void noObservationsGivesZeroBaseline() {
        ExponentialSmoothingForecaster.Baseline baseline = forecaster.computeBaseline(List.of(), 0.3);
        assertThat(baseline.dailyLevel()).isZero();
        assertThat(baseline.dailyStdDev()).isZero();
    }

    @Test
    void constantDailyQuantityConvergesToThatLevel() {
        Instant base = Instant.now();
        List<ExponentialSmoothingForecaster.DailyQuantity> daily = List.of(
                new ExponentialSmoothingForecaster.DailyQuantity(base, 10),
                new ExponentialSmoothingForecaster.DailyQuantity(base.plus(1, ChronoUnit.DAYS), 10),
                new ExponentialSmoothingForecaster.DailyQuantity(base.plus(2, ChronoUnit.DAYS), 10));
        ExponentialSmoothingForecaster.Baseline baseline = forecaster.computeBaseline(daily, 0.3);
        assertThat(baseline.dailyLevel()).isCloseTo(10.0, within(0.0001));
        assertThat(baseline.dailyStdDev()).isZero();
    }

    @Test
    void bucketByDaySumsSameDayEventsAndSeparatesDifferentDays() {
        Instant day1 = Instant.parse("2026-01-01T02:00:00Z");
        Instant day1Later = Instant.parse("2026-01-01T18:00:00Z");
        Instant day2 = Instant.parse("2026-01-02T05:00:00Z");

        List<ExponentialSmoothingForecaster.RawEvent> events = List.of(
                new ExponentialSmoothingForecaster.RawEvent(3, day1),
                new ExponentialSmoothingForecaster.RawEvent(4, day1Later),
                new ExponentialSmoothingForecaster.RawEvent(5, day2));

        List<ExponentialSmoothingForecaster.DailyQuantity> daily = forecaster.bucketByDay(events);

        assertThat(daily).hasSize(2);
        assertThat(daily.get(0).quantity()).isEqualTo(7);
        assertThat(daily.get(1).quantity()).isEqualTo(5);
    }
}
