package com.smartgrid.vendorservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record VendorSkuRequest(
        @NotBlank String skuId,
        @Positive double price,
        @PositiveOrZero int leadTimeDays
) {
}
