package com.smartgrid.analyticsservice.service;

import com.smartgrid.analyticsservice.dto.DisruptionSummaryResponse;
import com.smartgrid.analyticsservice.dto.OrderThroughputResponse;
import com.smartgrid.analyticsservice.dto.RerouteSuccessRateResponse;
import com.smartgrid.analyticsservice.dto.VendorPerformanceResponse;
import com.smartgrid.analyticsservice.search.OrderAnalyticsDocument;
import com.smartgrid.analyticsservice.search.OrderAnalyticsRepository;
import com.smartgrid.analyticsservice.search.VendorAnalyticsDocument;
import com.smartgrid.analyticsservice.search.VendorAnalyticsRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsQueryService {

    private static final Pageable RECENT_WINDOW_PAGE = PageRequest.of(0, 500);

    private final OrderAnalyticsRepository orderAnalyticsRepository;
    private final VendorAnalyticsRepository vendorAnalyticsRepository;

    public AnalyticsQueryService(OrderAnalyticsRepository orderAnalyticsRepository,
            VendorAnalyticsRepository vendorAnalyticsRepository) {
        this.orderAnalyticsRepository = orderAnalyticsRepository;
        this.vendorAnalyticsRepository = vendorAnalyticsRepository;
    }

    public List<DisruptionSummaryResponse> disruptionsSummary() {
        List<OrderAnalyticsDocument> tumbling = orderAnalyticsRepository
                .findByMetricTypeAndWindowTypeOrderByWindowStartDesc("disruption_count", "TUMBLING", RECENT_WINDOW_PAGE);
        List<OrderAnalyticsDocument> hopping = orderAnalyticsRepository
                .findByMetricTypeAndWindowTypeOrderByWindowStartDesc("disruption_count", "HOPPING", RECENT_WINDOW_PAGE);

        return java.util.stream.Stream.concat(latestPerRegion(tumbling), latestPerRegion(hopping))
                .map(doc -> new DisruptionSummaryResponse(doc.getDimensions().get("region"), doc.getWindowType(),
                        doc.getWindowStart(), doc.getWindowEnd(), doc.getValue()))
                .toList();
    }

    public List<OrderThroughputResponse> ordersThroughput() {
        List<OrderAnalyticsDocument> created = orderAnalyticsRepository
                .findByMetricTypeAndWindowTypeOrderByWindowStartDesc("order_created_count", "TUMBLING", RECENT_WINDOW_PAGE);
        List<OrderAnalyticsDocument> fulfilled = orderAnalyticsRepository
                .findByMetricTypeAndWindowTypeOrderByWindowStartDesc("order_throughput", "TUMBLING", RECENT_WINDOW_PAGE);
        List<OrderAnalyticsDocument> delivered = orderAnalyticsRepository
                .findByMetricTypeAndWindowTypeOrderByWindowStartDesc("shipment_delivered_count", "TUMBLING", RECENT_WINDOW_PAGE);

        return java.util.stream.Stream.of(
                        toThroughputResponses(created, "CREATED"),
                        toThroughputResponses(fulfilled, "FULFILLED"),
                        toThroughputResponses(delivered, "DELIVERED"))
                .flatMap(java.util.function.Function.identity())
                .toList();
    }

    public List<VendorPerformanceResponse> vendorsPerformance() {
        Map<String, VendorAnalyticsDocument> latestBreachByVendor = latestPerVendor(vendorAnalyticsRepository
                .findByMetricTypeAndWindowTypeOrderByWindowStartDesc("sla_breach_count", "HOPPING", RECENT_WINDOW_PAGE));
        Map<String, VendorAnalyticsDocument> latestScoreByVendor = latestPerVendor(vendorAnalyticsRepository
                .findByMetricTypeAndWindowTypeOrderByWindowStartDesc("vendor_score", "TUMBLING", RECENT_WINDOW_PAGE));

        return java.util.stream.Stream.concat(latestBreachByVendor.keySet().stream(), latestScoreByVendor.keySet().stream())
                .distinct()
                .map(vendorId -> new VendorPerformanceResponse(vendorId,
                        latestBreachByVendor.containsKey(vendorId) ? latestBreachByVendor.get(vendorId).getValue() : null,
                        latestScoreByVendor.containsKey(vendorId) ? latestScoreByVendor.get(vendorId).getValue() : null))
                .toList();
    }

    public RerouteSuccessRateResponse reroutesSuccessRate() {
        double successCount = orderAnalyticsRepository
                .findByMetricTypeAndWindowTypeOrderByWindowStartDesc("reroute_success_count", "TUMBLING", RECENT_WINDOW_PAGE)
                .stream().findFirst().map(OrderAnalyticsDocument::getValue).orElse(0.0);
        double escalationCount = orderAnalyticsRepository
                .findByMetricTypeAndWindowTypeOrderByWindowStartDesc("reroute_escalation_count", "TUMBLING", RECENT_WINDOW_PAGE)
                .stream().findFirst().map(OrderAnalyticsDocument::getValue).orElse(0.0);

        double total = successCount + escalationCount;
        double rate = total == 0.0 ? 0.0 : successCount / total;
        return new RerouteSuccessRateResponse(successCount, escalationCount, rate);
    }

    private java.util.stream.Stream<OrderThroughputResponse> toThroughputResponses(List<OrderAnalyticsDocument> docs, String stage) {
        return latestPerRegion(docs)
                .map(doc -> new OrderThroughputResponse(stage,
                        doc.getDimensions().getOrDefault("region", "all"),
                        doc.getWindowStart(), doc.getWindowEnd(), doc.getValue()));
    }

    private java.util.stream.Stream<OrderAnalyticsDocument> latestPerRegion(List<OrderAnalyticsDocument> docs) {
        return docs.stream()
                .collect(Collectors.toMap(doc -> doc.getDimensions().getOrDefault("region", "all"), doc -> doc,
                        (a, b) -> a.getWindowStart() >= b.getWindowStart() ? a : b))
                .values().stream()
                .sorted(Comparator.comparing(doc -> doc.getDimensions().getOrDefault("region", "all")));
    }

    private Map<String, VendorAnalyticsDocument> latestPerVendor(List<VendorAnalyticsDocument> docs) {
        return docs.stream()
                .collect(Collectors.toMap(doc -> doc.getDimensions().get("vendorId"), doc -> doc,
                        (a, b) -> a.getWindowStart() >= b.getWindowStart() ? a : b));
    }
}
