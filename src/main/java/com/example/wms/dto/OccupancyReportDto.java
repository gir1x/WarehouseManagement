package com.example.wms.dto;

import java.util.UUID;

public record OccupancyReportDto(
        UUID warehouseId,
        long totalSlots,
        long filledSlots,
        long availableSlots,
        double utilizationPercent
) {}
