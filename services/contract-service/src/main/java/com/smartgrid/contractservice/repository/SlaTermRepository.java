package com.smartgrid.contractservice.repository;

import com.smartgrid.contractservice.domain.SlaTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SlaTermRepository extends JpaRepository<SlaTerm, UUID> {

    /**
     * No external market-data feed exists anywhere in this system — "market benchmarks" are
     * computed as aggregate statistics across every real SLA term currently on file for this
     * metric, not fabricated external data.
     */
    @Query("SELECT new com.smartgrid.contractservice.repository.SlaTermBenchmark(" +
            "AVG(t.thresholdValue), AVG(t.penaltyPerBreach), COUNT(t)) " +
            "FROM SlaTerm t WHERE t.metricName = :metricName")
    Optional<SlaTermBenchmark> benchmarkFor(@Param("metricName") String metricName);
}
