package com.example.wms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ReceiveRequest(
        @NotBlank String productSku,
        @Min(1) int quantity,
        UUID slotId // optional — omit or send null to auto-assign the first available slot
) {
    public ReceiveCommand toCommand(UUID warehouseId, String performedBy) {
        return new ReceiveCommand(warehouseId, productSku, quantity, slotId, performedBy);
    }
}
