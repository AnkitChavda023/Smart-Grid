package com.smartgrid.forecasteragent.repository;

import com.smartgrid.forecasteragent.domain.DemandForecast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DemandForecastRepository extends JpaRepository<DemandForecast, UUID> {
    List<DemandForecast> findBySkuIdOrderByCreatedAtDesc(String skuId);
}
