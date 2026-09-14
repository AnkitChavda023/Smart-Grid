package com.smartgrid.analyticsservice.search;

import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface VendorAnalyticsRepository extends ElasticsearchRepository<VendorAnalyticsDocument, String> {

    List<VendorAnalyticsDocument> findByMetricTypeAndWindowTypeOrderByWindowStartDesc(
            String metricType, String windowType, Pageable pageable);
}
