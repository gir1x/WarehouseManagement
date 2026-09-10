package com.example.wms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record WarehouseRequest(
        @NotBlank String name,
        @Min(1) int aisles,
        @Min(1) int tiers
) {}
