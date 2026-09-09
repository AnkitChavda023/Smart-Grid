package com.smartgrid.slabreachagent.service;

import org.springframework.stereotype.Component;

/** Models SLA breaches as a Poisson process: rate lambda = observed breaches / window days, P(>=1 breach in horizon) = 1 - e^(-lambda * horizon). */
@Component
public class PoissonBreachAnalyzer {

    public double breachProbability(int breachCountInWindow, int windowDays, int forecastHorizonDays) {
        if (windowDays <= 0) {
            return 0.0;
        }
        double lambdaPerDay = (double) breachCountInWindow / windowDays;
        double expectedBreachesInHorizon = lambdaPerDay * forecastHorizonDays;
        return 1.0 - Math.exp(-expectedBreachesInHorizon);
    }
}
