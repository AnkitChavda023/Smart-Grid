package com.smartgrid.slabreachagent.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PoissonBreachAnalyzerTest {

    private final PoissonBreachAnalyzer analyzer = new PoissonBreachAnalyzer();

    @Test
    void zeroObservedBreachesGivesZeroProbability() {
        assertThat(analyzer.breachProbability(0, 90, 30)).isZero();
    }

    @Test
    void higherBreachCountGivesHigherProbability() {
        double low = analyzer.breachProbability(1, 90, 30);
        double high = analyzer.breachProbability(10, 90, 30);
        assertThat(high).isGreaterThan(low);
    }

    @Test
    void matchesClosedFormPoissonFormula() {
        // lambda = 9/90 = 0.1 per day; over a 30-day horizon, expected = 3; P(>=1) = 1 - e^-3
        double expected = 1.0 - Math.exp(-3.0);
        assertThat(analyzer.breachProbability(9, 90, 30)).isCloseTo(expected, within(1e-9));
    }

    @Test
    void probabilityNeverExceedsOne() {
        assertThat(analyzer.breachProbability(1000, 90, 90)).isLessThanOrEqualTo(1.0);
    }
}
