package com.smartgrid.inventoryservice.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record SafetyStockRequest(@PositiveOrZero long safetyStockLevel) {
}
