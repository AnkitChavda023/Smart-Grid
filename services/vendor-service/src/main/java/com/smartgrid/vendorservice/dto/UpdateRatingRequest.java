package com.smartgrid.vendorservice.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record UpdateRatingRequest(
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double reliabilityScore
) {
}
