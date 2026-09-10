package com.example.wms.service.allocation;

import com.example.wms.domain.Slot;

import java.util.UUID;

/**
 * STRATEGY PATTERN
 * ----------------
 * "Pick an empty slot" is a rule that's likely to grow (nearest-to-dock,
 * zone-based, weight-based, etc). InventoryService depends only on this
 * interface, so a new rule is a new class — never a change to InventoryService.
 */
public interface SlotAllocationStrategy {
    Slot findSlot(UUID warehouseId);
}
