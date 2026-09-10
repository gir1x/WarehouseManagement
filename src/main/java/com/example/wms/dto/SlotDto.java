package com.example.wms.dto;

import com.example.wms.domain.Slot;

import java.util.UUID;

public record SlotDto(UUID id, int aisle, int tier, String status, String productSku, Integer quantity) {
    public static SlotDto from(Slot slot) {
        String sku = slot.getItemStock() != null ? slot.getItemStock().getProduct().getSku() : null;
        Integer qty = slot.getItemStock() != null ? slot.getItemStock().getQuantity() : null;
        return new SlotDto(slot.getId(), slot.getAisle(), slot.getTier(), slot.getStatus().name(), sku, qty);
    }
}
