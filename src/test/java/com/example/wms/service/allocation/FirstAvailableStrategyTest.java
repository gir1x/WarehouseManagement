package com.example.wms.service.allocation;

import com.example.wms.domain.Slot;
import com.example.wms.domain.Warehouse;
import com.example.wms.exception.NoSlotAvailableException;
import com.example.wms.repository.SlotReader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * First suggested test from the documentation's Testing Strategy (§11):
 * the allocation Strategy returns an empty slot when one exists, and
 * throws when none exist.
 */
class FirstAvailableStrategyTest {

    private final SlotReader slotReader = mock(SlotReader.class);
    private final FirstAvailableStrategy strategy = new FirstAvailableStrategy(slotReader);

    @Test
    void returnsFirstEmptySlot() {
        UUID warehouseId = UUID.randomUUID();
        Warehouse warehouse = new Warehouse.Builder().name("Test WH").aisles(1).tiers(2).build();
        List<Slot> slots = warehouse.getSlots(); // [ (1,1) EMPTY, (1,2) EMPTY ]

        when(slotReader.findAllByWarehouse_Id(warehouseId)).thenReturn(slots);

        Slot result = strategy.findSlot(warehouseId);

        assertThat(result.getAisle()).isEqualTo(1);
        assertThat(result.getTier()).isEqualTo(1);
    }

    @Test
    void throwsWhenNoSlotIsEmpty() {
        UUID warehouseId = UUID.randomUUID();
        when(slotReader.findAllByWarehouse_Id(warehouseId)).thenReturn(List.of());

        assertThatThrownBy(() -> strategy.findSlot(warehouseId))
                .isInstanceOf(NoSlotAvailableException.class);
    }
}
