package com.smartgrid.pricingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record PriceRuleRequest(
        @NotBlank String skuId,
        @Positive double price,
        @NotNull LocalDate validFrom,
        @NotNull LocalDate validTo
) {
}
