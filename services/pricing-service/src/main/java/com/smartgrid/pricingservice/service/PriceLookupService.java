package com.smartgrid.pricingservice.service;

import com.smartgrid.commons.exception.ResourceNotFoundException;
import com.smartgrid.pricingservice.domain.PriceRule;
import com.smartgrid.pricingservice.repository.PriceRuleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class PriceLookupService {

    private final PriceRuleRepository priceRuleRepository;

    public PriceLookupService(PriceRuleRepository priceRuleRepository) {
        this.priceRuleRepository = priceRuleRepository;
    }

    public double currentPrice(String skuId, LocalDate date) {
        List<PriceRule> rules = priceRuleRepository.findBySkuId(skuId);
        if (rules.isEmpty()) {
            throw new ResourceNotFoundException("PriceRule", skuId);
        }

        PriceRuleIntervalTree tree = new PriceRuleIntervalTree();
        rules.forEach(tree::insert);

        List<PriceRule> applicable = tree.findOverlapping(date);
        if (applicable.isEmpty()) {
            throw new ResourceNotFoundException("PriceRule active on " + date + " for sku", skuId);
        }

        // If more than one rule's validity window overlaps today, the most recently started one wins.
        return applicable.stream()
                .max(Comparator.comparing(PriceRule::getValidFrom))
                .map(PriceRule::getPrice)
                .orElseThrow();
    }
}
