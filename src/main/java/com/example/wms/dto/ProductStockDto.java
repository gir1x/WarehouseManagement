package com.example.wms.dto;

/** Total on-hand quantity for one product across every warehouse, vs. its reorder threshold. */
public record ProductStockDto(
        String sku,
        String name,
        int totalOnHand,
        String unit,
        int reorderThreshold,
        boolean lowStock
) {}
