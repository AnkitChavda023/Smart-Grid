package com.smartgrid.forecasteragent.service;

import com.smartgrid.commons.metrics.AgentMetricsRecorder;
import com.smartgrid.commons.util.Confidence;
import com.smartgrid.forecasteragent.client.InventoryClient;
import com.smartgrid.forecasteragent.client.McpClient;
import com.smartgrid.forecasteragent.client.RagClient;
import com.smartgrid.forecasteragent.config.DemandForecasterProperties;
import com.smartgrid.forecasteragent.domain.DemandForecast;
import com.smartgrid.forecasteragent.llm.SeasonalAdjustment;
import com.smartgrid.forecasteragent.llm.SeasonalAdjustmentAnalyzer;
import com.smartgrid.forecasteragent.messaging.DemandForecastEventPublisher;
import com.smartgrid.forecasteragent.messaging.SafetyStockUpdateEventPublisher;
import com.smartgrid.forecasteragent.repository.DemandForecastRepository;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DemandForecastService {

    private static final Logger log = LoggerFactory.getLogger(DemandForecastService.class);

    /** z-score for the 90th/10th percentile of a normal distribution. */
    private static final double Z_90 = 1.2816;
    private static final int[] HORIZONS_DAYS = {7, 30, 90};

    private final InventoryClient inventoryClient;
    private final RagClient ragClient;
    private final McpClient mcpClient;
    private final SeasonalAdjustmentAnalyzer analyzer;
    private final ExponentialSmoothingForecaster smoother;
    private final DemandForecastRepository forecastRepository;
    private final DemandForecastEventPublisher forecastPublisher;
    private final SafetyStockUpdateEventPublisher safetyStockPublisher;
    private final DemandForecasterProperties properties;
    private final AgentMetricsRecorder metrics;

    public DemandForecastService(InventoryClient inventoryClient, RagClient ragClient, McpClient mcpClient,
                                  SeasonalAdjustmentAnalyzer analyzer, ExponentialSmoothingForecaster smoother,
                                  DemandForecastRepository forecastRepository, DemandForecastEventPublisher forecastPublisher,
                                  SafetyStockUpdateEventPublisher safetyStockPublisher,
                                  DemandForecasterProperties properties, AgentMetricsRecorder metrics) {
        this.inventoryClient = inventoryClient;
        this.ragClient = ragClient;
        this.mcpClient = mcpClient;
        this.analyzer = analyzer;
        this.smoother = smoother;
        this.forecastRepository = forecastRepository;
        this.forecastPublisher = forecastPublisher;
        this.safetyStockPublisher = safetyStockPublisher;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void forecastAllSkus() {
        for (String skuId : inventoryClient.allSkuIds()) {
            try {
                forecastForSku(skuId);
            } catch (Exception e) {
                log.warn("Daily forecast failed for skuId={}, continuing with remaining SKUs: {}", skuId, e.getMessage());
            }
        }
    }

    @Transactional
    public List<DemandForecast> forecastForSku(String skuId) {
        Timer.Sample latency = metrics.startLatency();
        try {
            List<InventoryClient.DemandEvent> events = inventoryClient.demandEventsFor(skuId);
            List<ExponentialSmoothingForecaster.DailyQuantity> daily = smoother.bucketByDay(
                    events.stream().map(e -> new ExponentialSmoothingForecaster.RawEvent(e.quantity(), e.occurredAt())).toList());
            ExponentialSmoothingForecaster.Baseline baseline = smoother.computeBaseline(daily, properties.smoothingAlpha());

            SeasonalAdjustment adjustment = analyzeWithFallback(skuId, baseline, daily.size());
            double confidence = Confidence.clamp(adjustment.confidence());
            metrics.recordConfidence(confidence);

            double effectiveFactor = 1.0;
            if (confidence >= properties.confidenceThreshold()) {
                effectiveFactor = adjustment.adjustmentFactor();
            } else {
                metrics.recordEscalation();
            }
            double adjustedDailyLevel = Math.max(0.0, baseline.dailyLevel() * effectiveFactor);

            List<DemandForecast> forecasts = new java.util.ArrayList<>();
            for (int horizonDays : HORIZONS_DAYS) {
                double mean = adjustedDailyLevel * horizonDays;
                double std = baseline.dailyStdDev() * Math.sqrt(horizonDays);
                double p10 = Math.max(0.0, mean - Z_90 * std);
                double p50 = mean;
                double p90 = mean + Z_90 * std;

                DemandForecast forecast = new DemandForecast(UUID.randomUUID(), skuId, horizonDays, p10, p50, p90,
                        confidence, adjustment.summary());
                forecastRepository.save(forecast);
                forecastPublisher.publish(forecast);
                forecasts.add(forecast);
            }

            applySafetyStockRecommendation(skuId, forecasts);
            alertIfReorderNeeded(skuId, forecasts);

            return forecasts;
        } finally {
            metrics.stopLatency(latency);
        }
    }

    /** Recommends the 7-day P90 as the safety stock buffer — enough to cover demand uncertainty over a short replenishment cycle. */
    private void applySafetyStockRecommendation(String skuId, List<DemandForecast> forecasts) {
        forecasts.stream().filter(f -> f.getHorizonDays() == 7).findFirst().ifPresent(sevenDay -> {
            long recommended = Math.round(sevenDay.getP90());
            for (String warehouseId : inventoryClient.warehousesFor(skuId)) {
                metrics.recordToolCall("updateSafetyStockLevel");
                mcpClient.call("updateSafetyStockLevel", 1, Map.of(
                        "skuId", skuId, "warehouseId", warehouseId, "safetyStockLevel", recommended), Object.class);
                safetyStockPublisher.publish(skuId, warehouseId, recommended,
                        "7-day P90 demand forecast: " + String.format("%.1f", sevenDay.getP90()));
            }
        });
    }

    /** Flags a real projected stockout: current available stock can't cover the median 30-day demand forecast. */
    private void alertIfReorderNeeded(String skuId, List<DemandForecast> forecasts) {
        forecasts.stream().filter(f -> f.getHorizonDays() == 30).findFirst().ifPresent(thirtyDay -> {
            long available = inventoryClient.totalAvailable(skuId);
            if (available < thirtyDay.getP50()) {
                metrics.recordToolCall("alertPlannerForReorder");
                mcpClient.call("alertPlannerForReorder", 1, Map.of(
                        "title", "Reorder recommended for SKU " + skuId,
                        "body", "Available stock " + available + " is below the median 30-day demand forecast of "
                                + String.format("%.1f", thirtyDay.getP50()) + ".",
                        "relatedEntityId", skuId), Object.class);
            }
        });
    }

    private SeasonalAdjustment analyzeWithFallback(String skuId, ExponentialSmoothingForecaster.Baseline baseline, int observedDays) {
        List<String> ragContext = searchRagSafely("demand seasonality context for SKU " + skuId, 3);
        String prompt = """
                SKU: %s
                Statistical baseline (exponential smoothing over %d observed days): daily level %.2f, daily std dev %.2f

                Relevant retrieved context:
                %s
                """.formatted(skuId, observedDays, baseline.dailyLevel(), baseline.dailyStdDev(),
                ragContext.isEmpty() ? "(none retrieved)" : String.join("\n---\n", ragContext));

        try {
            return analyzer.analyze(prompt);
        } catch (Exception first) {
            log.warn("Seasonal adjustment LLM call failed for skuId={}, retrying once: {}", skuId, first.getMessage());
            try {
                return analyzer.analyze(prompt);
            } catch (Exception second) {
                log.warn("Retry also failed for skuId={}, falling back to unadjusted baseline: {}", skuId, second.getMessage());
                return new SeasonalAdjustment(1.0, 0.50,
                        "Rule-based fallback (LLM unavailable): using unadjusted statistical baseline");
            }
        }
    }

    /** See DisruptionAnalysisService.searchRagSafely for why this is needed: RAG search can fail for the
     * same OpenAI-billing reason the LLM call above already has a fallback for, but it runs before that
     * fallback chain, so an unguarded failure here would crash the whole forecast instead of degrading. */
    private List<String> searchRagSafely(String query, int k) {
        try {
            return ragClient.search(query, k);
        } catch (Exception e) {
            log.warn("RAG search failed, proceeding with no historical context: {}", e.getMessage());
            return List.of();
        }
    }
}
