package com.example.wms.dto;

import java.util.UUID;

/** One warehouse's occupancy at a glance, plus whether it's approaching full capacity. */
public record WarehouseSummaryDto(
        UUID id,
        String name,
        long totalSlots,
        long filledSlots,
        long availableSlots,
        double utilizationPercent,
        boolean nearFull
) {}
