package com.smartgrid.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank String requestedBy,
        @NotBlank String destinationRegion,
        @NotEmpty @Valid List<OrderItemRequest> items
) {
}
