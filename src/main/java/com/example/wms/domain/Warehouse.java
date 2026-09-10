package com.example.wms.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * BUILDER PATTERN
 * ---------------
 * Creating a Warehouse isn't a simple 3-field constructor: it has to validate
 * the grid size AND generate every Slot in the grid before the object can be
 * considered "built". The Builder makes that setup explicit and guarantees
 * the Warehouse is never left half-constructed.
 */
@Entity
public class Warehouse {

    @Id
    @GeneratedValue
    private UUID id;

    private String name;
    private int totalAisles;
    private int totalTiers;

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Slot> slots = new ArrayList<>();

    protected Warehouse() {
        // JPA only
    }

    private Warehouse(Builder builder) {
        this.name = builder.name;
        this.totalAisles = builder.totalAisles;
        this.totalTiers = builder.totalTiers;
        this.slots = generateGrid(builder.totalAisles, builder.totalTiers);
    }

    private List<Slot> generateGrid(int aisles, int tiers) {
        List<Slot> generated = new ArrayList<>();
        for (int a = 1; a <= aisles; a++) {
            for (int t = 1; t <= tiers; t++) {
                generated.add(new Slot(this, a, t));
            }
        }
        return generated;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public int getTotalAisles() { return totalAisles; }
    public int getTotalTiers() { return totalTiers; }
    public List<Slot> getSlots() { return slots; }

    public static class Builder {
        private String name;
        private int totalAisles;
        private int totalTiers;

        public Builder name(String name) { this.name = name; return this; }
        public Builder aisles(int totalAisles) { this.totalAisles = totalAisles; return this; }
        public Builder tiers(int totalTiers) { this.totalTiers = totalTiers; return this; }

        public Warehouse build() {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Warehouse name is required");
            }
            if (totalAisles <= 0 || totalTiers <= 0) {
                throw new IllegalArgumentException("Aisles and tiers must both be positive");
            }
            return new Warehouse(this);
        }
    }
}
