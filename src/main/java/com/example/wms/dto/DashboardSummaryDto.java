package com.example.wms.dto;

import java.util.List;

/** Everything the dashboard overview page needs in one call. */
public record DashboardSummaryDto(
        long totalWarehouses,
        long totalProducts,
        long totalSlots,
        long filledSlots,
        long availableSlots,
        double utilizationPercent,
        List<WarehouseSummaryDto> warehouses,
        List<ProductStockDto> lowStockProducts,
        List<StockMovementDto> recentActivity
) {}
