package com.smartgrid.vendorevaluatoragent.service;

import com.smartgrid.vendorevaluatoragent.domain.VendorLeadTimeObservation;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class LeadTimeTrendAnalyzerTest {

    private final LeadTimeTrendAnalyzer analyzer = new LeadTimeTrendAnalyzer();

    /** observedAt defaults to Instant.now() at construction; tests need real day-spaced points, so it's overridden via reflection. */
    private static VendorLeadTimeObservation observationOn(int daysAgo, double leadTimeDays) {
        VendorLeadTimeObservation observation = new VendorLeadTimeObservation("v1", leadTimeDays);
        ReflectionTestUtils.setField(observation, "observedAt", Instant.now().minus(daysAgo, ChronoUnit.DAYS));
        return observation;
    }

    @Test
    void fewerThanTwoObservationsIsStableWithZeroSlope() {
        LeadTimeTrendAnalyzer.TrendResult result = analyzer.analyze(List.of(observationOn(0, 5.0)));
        assertThat(result.direction()).isEqualTo("STABLE");
        assertThat(result.slope()).isZero();
    }

    @Test
    void steadilyIncreasingLeadTimesAreDeclining() {
        List<VendorLeadTimeObservation> observations = List.of(
                observationOn(10, 5.0),
                observationOn(5, 10.0),
                observationOn(0, 15.0));
        LeadTimeTrendAnalyzer.TrendResult result = analyzer.analyze(observations);
        assertThat(result.direction()).isEqualTo("DECLINING");
        assertThat(result.slope()).isPositive();
    }

    @Test
    void constantLeadTimesAreStableWithNearZeroSlope() {
        List<VendorLeadTimeObservation> observations = List.of(
                observationOn(10, 7.0),
                observationOn(5, 7.0),
                observationOn(0, 7.0));
        LeadTimeTrendAnalyzer.TrendResult result = analyzer.analyze(observations);
        assertThat(result.direction()).isEqualTo("STABLE");
        assertThat(result.slope()).isCloseTo(0.0, within(0.0001));
    }
}
