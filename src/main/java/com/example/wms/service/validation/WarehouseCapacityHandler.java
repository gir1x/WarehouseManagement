package com.example.wms.service.validation;

import com.example.wms.domain.SlotStatus;
import com.example.wms.dto.ReceiveCommand;
import com.example.wms.exception.ValidationException;
import com.example.wms.repository.SlotReader;

// Not a @Component — see the note in ProductExistsHandler.java.
public class WarehouseCapacityHandler extends ReceiveValidationHandler {

    private final SlotReader slotReader;

    public WarehouseCapacityHandler(SlotReader slotReader) {
        this.slotReader = slotReader;
    }

    @Override
    protected void doValidate(ReceiveCommand command) {
        boolean hasSpace = slotReader.findAllByWarehouse_Id(command.warehouseId()).stream()
                .anyMatch(slot -> slot.getStatus() == SlotStatus.EMPTY);
        if (!hasSpace) {
            throw new ValidationException("Warehouse is at full capacity");
        }
    }
}
