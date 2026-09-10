package com.example.wms.service.allocation;

import com.example.wms.domain.Slot;
import com.example.wms.domain.SlotStatus;
import com.example.wms.exception.NoSlotAvailableException;
import com.example.wms.repository.SlotReader;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Today's only allocation rule: the first empty slot found. */
@Component
public class FirstAvailableStrategy implements SlotAllocationStrategy {

    private final SlotReader slotReader;

    public FirstAvailableStrategy(SlotReader slotReader) {
        this.slotReader = slotReader;
    }

    @Override
    public Slot findSlot(UUID warehouseId) {
        return slotReader.findAllByWarehouse_Id(warehouseId).stream()
                .filter(slot -> slot.getStatus() == SlotStatus.EMPTY)
                .findFirst()
                .orElseThrow(() -> new NoSlotAvailableException(warehouseId));
    }
}
