package com.smartgrid.forecasteragent.web;

import com.smartgrid.forecasteragent.domain.DemandForecast;
import com.smartgrid.forecasteragent.repository.DemandForecastRepository;
import com.smartgrid.forecasteragent.service.DemandForecastService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DemandForecastController {

    private final DemandForecastRepository forecastRepository;
    private final DemandForecastService forecastService;

    public DemandForecastController(DemandForecastRepository forecastRepository, DemandForecastService forecastService) {
        this.forecastRepository = forecastRepository;
        this.forecastService = forecastService;
    }

    @GetMapping("/demand-forecasts/{skuId}")
    public List<DemandForecast> getForecasts(@PathVariable String skuId) {
        return forecastRepository.findBySkuIdOrderByCreatedAtDesc(skuId);
    }

    /** Drives the same real forecast the daily schedule or a StockDepleted event would — for manual dev triggering, per the module spec's "POST /agents/{agentId}/simulate" requirement. */
    @PostMapping("/demand-forecasts/simulate")
    public List<DemandForecast> simulate(@Valid @RequestBody SimulateRequest request) {
        return forecastService.forecastForSku(request.skuId());
    }

    public record SimulateRequest(@NotBlank String skuId) {
    }
}
