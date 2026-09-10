package com.example.wms.dto;

import com.example.wms.domain.StockMovement;

import java.time.Instant;
import java.util.UUID;

/** One row of the audit log — a single receive or pick against one slot. */
public record StockMovementDto(
        UUID id,
        UUID warehouseId,
        String warehouseName,
        int aisle,
        int tier,
        String productSku,
        int quantity,
        String type,
        String performedBy,
        Instant occurredAt
) {
    public static StockMovementDto from(StockMovement movement) {
        return new StockMovementDto(
                movement.getId(),
                movement.getSlot().getWarehouse().getId(),
                movement.getSlot().getWarehouse().getName(),
                movement.getSlot().getAisle(),
                movement.getSlot().getTier(),
                movement.getProductSku(),
                movement.getQuantity(),
                movement.getType().name(),
                movement.getPerformedBy(),
                movement.getOccurredAt()
        );
    }
}
