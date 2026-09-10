package com.example.wms.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(
    name = "slot",
    uniqueConstraints = @UniqueConstraint(columnNames = {"warehouse_id", "aisle", "tier"})
    // This constraint is what actually prevents two users double-booking the same
    // slot under concurrency — the allocation Strategy just picks a candidate;
    // this constraint plus @Version below is the real safety net (see NFR-1).
)
public class Slot {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    private int aisle;
    private int tier;

    @Enumerated(EnumType.STRING)
    private SlotStatus status = SlotStatus.EMPTY;

    @OneToOne(mappedBy = "slot", cascade = CascadeType.ALL, orphanRemoval = true)
    private ItemStock itemStock;

    @Version
    private Long version; // optimistic locking — protects against two simultaneous receives

    protected Slot() {
        // JPA only
    }

    public Slot(Warehouse warehouse, int aisle, int tier) {
        this.warehouse = warehouse;
        this.aisle = aisle;
        this.tier = tier;
        this.status = SlotStatus.EMPTY;
    }

    // --- State pattern entry points -----------------------------------
    public void receive(ItemStock itemStock) {
        currentState().receive(this, itemStock);
    }

    public void pick(int quantity) {
        currentState().pick(this, quantity);
    }

    private SlotState currentState() {
        return this.status == SlotStatus.EMPTY ? new EmptyState() : new OccupiedState();
    }

    // package-private — only SlotState implementations should call these
    void setStatus(SlotStatus status) { this.status = status; }
    void setItemStock(ItemStock itemStock) {
        this.itemStock = itemStock;
        if (itemStock != null) {
            itemStock.setSlot(this);
        }
    }

    public UUID getId() { return id; }
    public Warehouse getWarehouse() { return warehouse; }
    public int getAisle() { return aisle; }
    public int getTier() { return tier; }
    public SlotStatus getStatus() { return status; }
    public ItemStock getItemStock() { return itemStock; }
    public Long getVersion() { return version; }
}
