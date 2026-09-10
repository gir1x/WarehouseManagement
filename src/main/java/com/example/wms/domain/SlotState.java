package com.example.wms.domain;

/**
 * STATE PATTERN
 * -------------
 * Each concrete state (EmptyState / OccupiedState) owns the rules for what's
 * allowed to happen to a Slot while it's in that state. Slot itself never
 * contains "if (status == OCCUPIED) throw ..." checks — it just asks its
 * current state to handle the request.
 *
 * pick() takes a quantity now (partial picks): OccupiedState reduces the
 * stock in place and stays OCCUPIED unless the requested quantity empties
 * it completely, in which case it transitions to EmptyState — same idea as
 * before, just quantity-aware.
 *
 * Note on the JPA adaptation: in the documentation's original sketch, a
 * SlotState instance was held as a field. Here, since Slot is a JPA entity
 * persisted by its `status` column, we derive the right SlotState object
 * from that column on demand (see Slot.currentState()) instead of storing
 * the state object itself — that keeps the entity simple to persist while
 * still keeping all the transition logic inside the State classes.
 */
public interface SlotState {
    void receive(Slot slot, ItemStock itemStock);
    void pick(Slot slot, int quantity);
}
