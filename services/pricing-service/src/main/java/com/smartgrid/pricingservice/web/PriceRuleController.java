package com.smartgrid.pricingservice.web;

import com.smartgrid.pricingservice.domain.PriceRule;
import com.smartgrid.pricingservice.dto.PriceRuleRequest;
import com.smartgrid.pricingservice.repository.PriceRuleRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/price-rules")
public class PriceRuleController {

    private final PriceRuleRepository priceRuleRepository;

    public PriceRuleController(PriceRuleRepository priceRuleRepository) {
        this.priceRuleRepository = priceRuleRepository;
    }

    @PostMapping
    public ResponseEntity<Void> createPriceRule(@Valid @RequestBody PriceRuleRequest request) {
        priceRuleRepository.save(new PriceRule(request.skuId(), request.price(), request.validFrom(), request.validTo()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
