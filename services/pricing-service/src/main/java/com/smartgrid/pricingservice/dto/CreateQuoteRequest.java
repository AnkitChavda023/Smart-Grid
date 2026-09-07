package com.smartgrid.pricingservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateQuoteRequest(
        @NotBlank String orderId,
        @NotBlank String vendorId,
        @NotEmpty @Valid List<QuoteItemRequest> items
) {
}
