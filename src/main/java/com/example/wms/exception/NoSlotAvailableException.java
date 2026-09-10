package com.example.wms.exception;

import java.util.UUID;

public class NoSlotAvailableException extends RuntimeException {
    public NoSlotAvailableException(UUID warehouseId) {
        super("No empty slot available in warehouse: " + warehouseId);
    }
}
