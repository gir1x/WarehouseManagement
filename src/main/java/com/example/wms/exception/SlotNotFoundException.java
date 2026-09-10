package com.example.wms.exception;

import java.util.UUID;

public class SlotNotFoundException extends RuntimeException {
    public SlotNotFoundException(UUID id) {
        super("Slot not found: " + id);
    }
}
