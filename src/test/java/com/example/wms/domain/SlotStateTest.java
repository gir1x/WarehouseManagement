package com.example.wms.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Second suggested test from the documentation's Testing Strategy (§11),
 * extended to cover partial picks: EmptyState.receive() transitions to
 * OccupiedState; OccupiedState.receive() throws; picking less than the full
 * stock reduces it in place and stays OCCUPIED; picking everything (or more
 * than what's there) behaves like before.
 */
class SlotStateTest {

    @Test
    void receivingIntoAnEmptySlotOccupiesIt() {
        Warehouse warehouse = new Warehouse.Builder().name("Test WH").aisles(1).tiers(1).build();
        Slot slot = warehouse.getSlots().get(0);
        Product product = new Product("SKU-TEST", "Test Product");
        ItemStock itemStock = new ItemStock(product, 5);

        slot.receive(itemStock);

        assertThat(slot.getStatus()).isEqualTo(SlotStatus.OCCUPIED);
        assertThat(slot.getItemStock()).isEqualTo(itemStock);
    }

    @Test
    void receivingIntoAnOccupiedSlotThrows() {
        Warehouse warehouse = new Warehouse.Builder().name("Test WH").aisles(1).tiers(1).build();
        Slot slot = warehouse.getSlots().get(0);
        slot.receive(new ItemStock(new Product("SKU-TEST", "Test Product"), 5));

        assertThatThrownBy(() -> slot.receive(new ItemStock(new Product("SKU-TEST-2", "Other"), 1)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void pickingFromAnEmptySlotThrows() {
        Warehouse warehouse = new Warehouse.Builder().name("Test WH").aisles(1).tiers(1).build();
        Slot slot = warehouse.getSlots().get(0);

        assertThatThrownBy(() -> slot.pick(1)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void pickingFewerThanTheFullStockReducesItAndStaysOccupied() {
        Warehouse warehouse = new Warehouse.Builder().name("Test WH").aisles(1).tiers(1).build();
        Slot slot = warehouse.getSlots().get(0);
        slot.receive(new ItemStock(new Product("SKU-TEST", "Test Product"), 10));

        slot.pick(3);

        assertThat(slot.getStatus()).isEqualTo(SlotStatus.OCCUPIED);
        assertThat(slot.getItemStock().getQuantity()).isEqualTo(7);
    }

    @Test
    void pickingExactlyTheFullStockEmptiesTheSlot() {
        Warehouse warehouse = new Warehouse.Builder().name("Test WH").aisles(1).tiers(1).build();
        Slot slot = warehouse.getSlots().get(0);
        slot.receive(new ItemStock(new Product("SKU-TEST", "Test Product"), 10));

        slot.pick(10);

        assertThat(slot.getStatus()).isEqualTo(SlotStatus.EMPTY);
        assertThat(slot.getItemStock()).isNull();
    }

    @Test
    void pickingMoreThanWhatsThereThrows() {
        Warehouse warehouse = new Warehouse.Builder().name("Test WH").aisles(1).tiers(1).build();
        Slot slot = warehouse.getSlots().get(0);
        slot.receive(new ItemStock(new Product("SKU-TEST", "Test Product"), 5));

        assertThatThrownBy(() -> slot.pick(6)).isInstanceOf(IllegalStateException.class);
        // stock is untouched after the rejected attempt
        assertThat(slot.getItemStock().getQuantity()).isEqualTo(5);
    }
}
