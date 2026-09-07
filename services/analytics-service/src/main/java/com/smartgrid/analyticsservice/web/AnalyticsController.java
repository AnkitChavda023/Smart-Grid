package com.smartgrid.analyticsservice.web;

import com.smartgrid.analyticsservice.dto.DisruptionSummaryResponse;
import com.smartgrid.analyticsservice.dto.OrderThroughputResponse;
import com.smartgrid.analyticsservice.dto.RerouteSuccessRateResponse;
import com.smartgrid.analyticsservice.dto.VendorPerformanceResponse;
import com.smartgrid.analyticsservice.service.AnalyticsQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsQueryService analyticsQueryService;

    public AnalyticsController(AnalyticsQueryService analyticsQueryService) {
        this.analyticsQueryService = analyticsQueryService;
    }

    @GetMapping("/disruptions/summary")
    public List<DisruptionSummaryResponse> disruptionsSummary() {
        return analyticsQueryService.disruptionsSummary();
    }

    @GetMapping("/vendors/performance")
    public List<VendorPerformanceResponse> vendorsPerformance(@RequestParam(defaultValue = "7d") String period) {
        return analyticsQueryService.vendorsPerformance();
    }

    @GetMapping("/orders/throughput")
    public List<OrderThroughputResponse> ordersThroughput() {
        return analyticsQueryService.ordersThroughput();
    }

    @GetMapping("/reroutes/success-rate")
    public RerouteSuccessRateResponse reroutesSuccessRate() {
        return analyticsQueryService.reroutesSuccessRate();
    }
}
