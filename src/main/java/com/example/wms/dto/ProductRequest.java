package com.example.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductRequest(
        @NotBlank String sku,
        @NotBlank String name,
        @PositiveOrZero Integer reorderThreshold,
        String unit
) {}
