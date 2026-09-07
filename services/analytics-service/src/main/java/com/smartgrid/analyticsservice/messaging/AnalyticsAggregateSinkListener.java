package com.smartgrid.analyticsservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartgrid.analyticsservice.domain.AnalyticsSnapshot;
import com.smartgrid.analyticsservice.repository.AnalyticsSnapshotRepository;
import com.smartgrid.analyticsservice.search.OrderAnalyticsDocument;
import com.smartgrid.analyticsservice.search.OrderAnalyticsRepository;
import com.smartgrid.analyticsservice.search.VendorAnalyticsDocument;
import com.smartgrid.analyticsservice.search.VendorAnalyticsRepository;
import com.smartgrid.commons.avro.analytics.AnalyticsAggregateComputed;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * CQRS materialization listener that writes computed Kafka Streams aggregates into
 * Elasticsearch (for live dashboard reads) and PostgreSQL (for point-in-time history).
 */
@Component
public class AnalyticsAggregateSinkListener {

    private static final Set<String> VENDOR_METRICS = new HashSet<>(Set.of("sla_breach_count", "vendor_score"));

    private final OrderAnalyticsRepository orderAnalyticsRepository;
    private final VendorAnalyticsRepository vendorAnalyticsRepository;
    private final AnalyticsSnapshotRepository analyticsSnapshotRepository;
    private final ObjectMapper objectMapper;

    public AnalyticsAggregateSinkListener(OrderAnalyticsRepository orderAnalyticsRepository,
            VendorAnalyticsRepository vendorAnalyticsRepository,
            AnalyticsSnapshotRepository analyticsSnapshotRepository, ObjectMapper objectMapper) {
        this.orderAnalyticsRepository = orderAnalyticsRepository;
        this.vendorAnalyticsRepository = vendorAnalyticsRepository;
        this.analyticsSnapshotRepository = analyticsSnapshotRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "analytics-aggregates", groupId = "${smartgrid.kafka.group-id}-sink")
    public void onAggregateComputed(ConsumerRecord<String, Object> record) {
        if (!(record.value() instanceof AnalyticsAggregateComputed aggregate)) {
            return;
        }

        Map<String, String> dimensions = new HashMap<>(aggregate.getDimensions());
        String dimensionKey = dimensions.values().stream().findFirst().orElse("global");
        String documentId = aggregate.getMetricType() + ":" + aggregate.getWindowType() + ":" + dimensionKey
                + ":" + aggregate.getWindowStart();

        if (VENDOR_METRICS.contains(aggregate.getMetricType())) {
            vendorAnalyticsRepository.save(new VendorAnalyticsDocument(documentId, aggregate.getMetricType(),
                    aggregate.getWindowType(), aggregate.getWindowStart(), aggregate.getWindowEnd(),
                    aggregate.getValue(), dimensions));
        } else {
            orderAnalyticsRepository.save(new OrderAnalyticsDocument(documentId, aggregate.getMetricType(),
                    aggregate.getWindowType(), aggregate.getWindowStart(), aggregate.getWindowEnd(),
                    aggregate.getValue(), dimensions));
        }

        analyticsSnapshotRepository.save(new AnalyticsSnapshot(aggregate.getMetricType(), aggregate.getWindowType(),
                aggregate.getWindowStart(), aggregate.getWindowEnd(), aggregate.getValue(), toJson(dimensions)));
    }

    private String toJson(Map<String, String> dimensions) {
        try {
            return objectMapper.writeValueAsString(dimensions);
        } catch (Exception e) {
            return "{}";
        }
    }
}
