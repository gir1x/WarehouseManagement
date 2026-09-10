package com.example.wms.dto;

import java.util.UUID;

/**
 * Internal command object passed to the validation chain and InventoryService.
 * preferredSlotId is nullable: when the user picked a specific empty slot on
 * the floor plan, InventoryService uses it directly (after validating it's
 * actually empty and belongs to this warehouse); when it's null, the
 * SlotAllocationStrategy (Strategy pattern) picks one automatically, same as
 * before — so existing API callers that don't send a slotId keep working.
 */
public record ReceiveCommand(UUID warehouseId, String productSku, int quantity, UUID preferredSlotId, String performedBy) {}
