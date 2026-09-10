package com.example.wms.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
public class Product {

    private static final int DEFAULT_REORDER_THRESHOLD = 5;
    private static final String DEFAULT_UNIT = "pcs";

    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true, nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    /**
     * Total on-hand quantity across all warehouses at or below this triggers a low-stock alert.
     * Deliberately nullable in the DB (an Integer wrapper, not int) so adding this column to a
     * table that already has rows doesn't fail with "contains null values" — existing rows just
     * get null here, and getReorderThreshold() below falls back to the default for them.
     */
    @Column(name = "reorder_threshold")
    private Integer reorderThreshold;

    /**
     * The unit this product's quantity is measured/consumed in — "pcs", "kg", "box", "litre",
     * etc. Purely descriptive: it's shown alongside every quantity in the UI (receive form,
     * floor tiles, dashboard) so "40" reads as "40 pcs" or "40 kg", whichever is true for that
     * product. It does not change how slot occupancy works — a slot is still either EMPTY or
     * OCCUPIED (see SlotState) regardless of what unit its contents are measured in.
     * Same nullable-with-default pattern as reorderThreshold, for the same reason.
     */
    @Column(name = "unit")
    private String unit;

    protected Product() {
        // JPA only
    }

    public Product(String sku, String name) {
        this(sku, name, DEFAULT_REORDER_THRESHOLD, DEFAULT_UNIT);
    }

    public Product(String sku, String name, Integer reorderThreshold) {
        this(sku, name, reorderThreshold, DEFAULT_UNIT);
    }

    public Product(String sku, String name, Integer reorderThreshold, String unit) {
        this.sku = sku;
        this.name = name;
        this.reorderThreshold = reorderThreshold != null ? reorderThreshold : DEFAULT_REORDER_THRESHOLD;
        this.unit = (unit != null && !unit.isBlank()) ? unit : DEFAULT_UNIT;
    }

    public UUID getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getReorderThreshold() { return reorderThreshold != null ? reorderThreshold : DEFAULT_REORDER_THRESHOLD; }
    public String getUnit() { return unit != null ? unit : DEFAULT_UNIT; }
}
