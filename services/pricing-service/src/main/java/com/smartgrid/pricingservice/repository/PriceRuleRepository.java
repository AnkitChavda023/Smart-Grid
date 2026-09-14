package com.smartgrid.pricingservice.repository;

import com.smartgrid.pricingservice.domain.PriceRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PriceRuleRepository extends JpaRepository<PriceRule, UUID> {

    List<PriceRule> findBySkuId(String skuId);
}
