package com.smartgrid.pricingservice.service;

import com.smartgrid.pricingservice.domain.PriceRule;
import com.smartgrid.pricingservice.repository.PriceRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class PriceRuleAdjustmentService {

    private static final double ADJUSTMENT_PER_SCORE_POINT = 0.10;

    private final PriceRuleRepository priceRuleRepository;

    public PriceRuleAdjustmentService(PriceRuleRepository priceRuleRepository) {
        this.priceRuleRepository = priceRuleRepository;
    }

    @Transactional
    public void recalculateForVendorScoreChange(String skuId, double previousScore, double newScore) {
        List<PriceRule> rules = priceRuleRepository.findBySkuId(skuId);
        if (rules.isEmpty()) {
            return;
        }

        PriceRuleIntervalTree tree = new PriceRuleIntervalTree();
        rules.forEach(tree::insert);

        LocalDate today = LocalDate.now();
        tree.findOverlapping(today).stream()
                .max(Comparator.comparing(PriceRule::getValidFrom))
                .ifPresent(rule -> {
                    double scoreDrop = previousScore - newScore;
                    double adjustmentFactor = 1.0 + (scoreDrop * ADJUSTMENT_PER_SCORE_POINT);
                    rule.setPrice(rule.getPrice() * adjustmentFactor);
                    priceRuleRepository.save(rule);
                });
    }
}
