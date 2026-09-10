package com.example.wms.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * FACTORY METHOD (small-scale)
 * -----------------------------
 * StockMovement.receive(...) / StockMovement.pick(...) centralize how each
 * movement type is constructed, instead of every caller writing out
 * `new StockMovement(..., MovementType.RECEIVE, ...)` by hand.
 *
 * Deliberately stores a productSku/quantity SNAPSHOT rather than a foreign
 * key to ItemStock — picking an item deletes its ItemStock row (orphanRemoval
 * on Slot.itemStock), so a movement record must not depend on that row still
 * existing for the audit trail to remain readable.
 */
@Entity
public class StockMovement {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id")
    private Slot slot;

    private String productSku;
    private int quantity;

    @Enumerated(EnumType.STRING)
    private MovementType type;

    private String performedBy;
    private Instant occurredAt;

    protected StockMovement() {
        // JPA only
    }

    private StockMovement(Slot slot, String productSku, int quantity, MovementType type, String performedBy) {
        this.slot = slot;
        this.productSku = productSku;
        this.quantity = quantity;
        this.type = type;
        this.performedBy = performedBy;
        this.occurredAt = Instant.now();
    }

    public static StockMovement receive(Slot slot, String productSku, int quantity, String performedBy) {
        return new StockMovement(slot, productSku, quantity, MovementType.RECEIVE, performedBy);
    }

    public static StockMovement pick(Slot slot, String productSku, int quantity, String performedBy) {
        return new StockMovement(slot, productSku, quantity, MovementType.PICK, performedBy);
    }

    public UUID getId() { return id; }
    public Slot getSlot() { return slot; }
    public String getProductSku() { return productSku; }
    public int getQuantity() { return quantity; }
    public MovementType getType() { return type; }
    public String getPerformedBy() { return performedBy; }
    public Instant getOccurredAt() { return occurredAt; }
}
