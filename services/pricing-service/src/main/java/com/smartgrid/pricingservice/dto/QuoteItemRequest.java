package com.smartgrid.pricingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record QuoteItemRequest(
        @NotBlank String skuId,
        @Positive int quantity
) {
}
