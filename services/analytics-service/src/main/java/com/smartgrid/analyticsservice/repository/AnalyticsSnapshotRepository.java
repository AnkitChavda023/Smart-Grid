package com.smartgrid.analyticsservice.repository;

import com.smartgrid.analyticsservice.domain.AnalyticsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnalyticsSnapshotRepository extends JpaRepository<AnalyticsSnapshot, UUID> {

    List<AnalyticsSnapshot> findByMetricTypeOrderByCapturedAtDesc(String metricType);
}
