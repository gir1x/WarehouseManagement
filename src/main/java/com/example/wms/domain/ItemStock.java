package com.example.wms.domain;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * A specific quantity of a Product sitting in one Slot.
 * Owns the relationship to Slot (holds the foreign key) — see Slot.itemStock (mappedBy).
 */
@Entity
public class ItemStock {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    private int quantity;

    @OneToOne
    @JoinColumn(name = "slot_id")
    private Slot slot;

    protected ItemStock() {
        // JPA only
    }

    public ItemStock(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public UUID getId() { return id; }
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public Slot getSlot() { return slot; }
    public void setSlot(Slot slot) { this.slot = slot; }

    /**
     * Called only from OccupiedState.pick() when a pick doesn't take the
     * whole stock. Same IllegalStateException convention as the rest of the
     * domain layer (see EmptyState/OccupiedState) — GlobalExceptionHandler
     * maps it to 409, same family as "that transition isn't allowed."
     */
    void reduceQuantity(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Pick quantity must be positive");
        }
        if (amount > quantity) {
            throw new IllegalStateException(
                    "Cannot pick " + amount + " — only " + quantity + " available in this slot");
        }
        this.quantity -= amount;
    }
}
