package com.example.wms.domain;

public class EmptyState implements SlotState {

    @Override
    public void receive(Slot slot, ItemStock itemStock) {
        slot.setItemStock(itemStock);
        slot.setStatus(SlotStatus.OCCUPIED);
    }

    @Override
    public void pick(Slot slot, int quantity) {
        throw new IllegalStateException("Cannot pick from an empty slot: " + slot.getId());
    }
}
