package com.example.wms.dto;

import java.util.UUID;

public record ReceiveResult(UUID slotId, int aisle, int tier) {}
