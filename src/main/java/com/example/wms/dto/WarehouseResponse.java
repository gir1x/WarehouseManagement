package com.example.wms.dto;

import com.example.wms.domain.Warehouse;

import java.util.UUID;

public record WarehouseResponse(UUID id, String name, int totalAisles, int totalTiers) {
    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getName(),
                warehouse.getTotalAisles(),
                warehouse.getTotalTiers()
        );
    }
}
