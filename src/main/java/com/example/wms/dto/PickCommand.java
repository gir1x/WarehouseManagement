package com.example.wms.dto;

import java.util.UUID;

public record PickCommand(UUID slotId, int quantity, String performedBy) {}
