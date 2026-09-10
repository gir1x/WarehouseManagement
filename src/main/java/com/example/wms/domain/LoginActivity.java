package com.example.wms.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * One row per successful sign-in — the audit trail the requirement calls
 * "the logs need to be noted." Deliberately stores just the username as a
 * plain string (same pattern as StockMovement.performedBy), not a foreign
 * key to User — a login record should survive even if the account is later
 * deleted, and it keeps this table decoupled from User's lifecycle.
 */
@Entity
public class LoginActivity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private Instant occurredAt;

    protected LoginActivity() {
        // JPA only
    }

    public LoginActivity(String username, Instant occurredAt) {
        this.username = username;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public Instant getOccurredAt() { return occurredAt; }
}
