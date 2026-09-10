package com.example.wms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PickRequest(
        @NotNull UUID slotId,
        @Min(1) int quantity
) {
    public PickCommand toCommand(String performedBy) {
        return new PickCommand(slotId, quantity, performedBy);
    }
}
