package com.example.wms.dto;

import java.util.UUID;

/**
 * A "record" is a shorthand Java class for data that never changes after
 * it's created — Java auto-generates the constructor, getters (slotId(),
 * aisle(), tier()), equals(), and toString() for you. This one is just what
 * we send back to the browser after a successful pick.
 */
public record PickResult(UUID slotId, int aisle, int tier) {}
