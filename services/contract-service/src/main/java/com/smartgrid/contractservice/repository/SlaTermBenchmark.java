package com.smartgrid.contractservice.repository;

public class SlaTermBenchmark {

    private final Double averageThreshold;
    private final Double averagePenalty;
    private final Long contractCount;

    public SlaTermBenchmark(Double averageThreshold, Double averagePenalty, Long contractCount) {
        this.averageThreshold = averageThreshold;
        this.averagePenalty = averagePenalty;
        this.contractCount = contractCount;
    }

    public Double getAverageThreshold() {
        return averageThreshold;
    }

    public Double getAveragePenalty() {
        return averagePenalty;
    }

    public Long getContractCount() {
        return contractCount;
    }
}
