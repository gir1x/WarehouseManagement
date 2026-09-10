package com.example.wms.domain;

public class OccupiedState implements SlotState {

    @Override
    public void receive(Slot slot, ItemStock itemStock) {
        throw new IllegalStateException("Slot is already occupied: " + slot.getId());
    }

    @Override
    public void pick(Slot slot, int quantity) {
        ItemStock stock = slot.getItemStock();

        if (quantity == stock.getQuantity()) {
            // Picking everything empties the slot, same as before.
            slot.setItemStock(null);
            slot.setStatus(SlotStatus.EMPTY);
        } else {
            // Partial pick: reduce the stock in place, slot stays OCCUPIED.
            // reduceQuantity() itself rejects quantity > stock.getQuantity().
            stock.reduceQuantity(quantity);
        }
    }
}
