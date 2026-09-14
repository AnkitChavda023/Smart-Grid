package com.smartgrid.contractservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record SlaTermRequest(
        @NotBlank String metricName,
        @Positive double thresholdValue,
        @PositiveOrZero double penaltyPerBreach
) {
}
