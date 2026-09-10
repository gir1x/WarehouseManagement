package com.example.wms.domain;

/** The two states a Slot can be in. Drives the State pattern in SlotState/EmptyState/OccupiedState. */
public enum SlotStatus {
    EMPTY,
    OCCUPIED
}
