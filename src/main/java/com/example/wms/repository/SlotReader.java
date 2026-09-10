package com.example.wms.repository;

import com.example.wms.domain.Slot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * INTERFACE SEGREGATION
 * ---------------------
 * Read-only slot queries, split out from write operations. Classes that only
 * ever need to look at slots (SlotAllocationStrategy, ReportService) depend
 * on this narrow interface instead of the full SlotRepository — they aren't
 * forced to depend on save/delete methods they never call.
 */
public interface SlotReader {
    Optional<Slot> findByWarehouse_IdAndAisleAndTier(UUID warehouseId, int aisle, int tier);
    List<Slot> findAllByWarehouse_Id(UUID warehouseId);
}
