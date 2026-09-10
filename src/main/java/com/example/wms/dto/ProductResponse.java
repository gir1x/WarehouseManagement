package com.example.wms.dto;

import com.example.wms.domain.Product;

import java.util.UUID;

public record ProductResponse(UUID id, String sku, String name, int reorderThreshold, String unit) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getSku(), product.getName(),
                product.getReorderThreshold(), product.getUnit());
    }
}
