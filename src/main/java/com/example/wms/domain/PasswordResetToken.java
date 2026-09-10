package com.example.wms.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * A one-time, time-limited token emailed to a user who asked to reset their
 * password. Deliberately a random opaque string (not something derived from
 * the user's id or email) so it can't be guessed, and single-use (see
 * markUsed()) so replaying an old email doesn't reset the password twice.
 */
@Entity
public class PasswordResetToken {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(unique = true, nullable = false)
    private String token;

    private Instant expiresAt;

    private boolean used = false;

    protected PasswordResetToken() {
        // JPA only
    }

    public PasswordResetToken(User user, String token, Instant expiresAt) {
        this.user = user;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public boolean isUsable() {
        return !used && Instant.now().isBefore(expiresAt);
    }

    public void markUsed() {
        this.used = true;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getToken() { return token; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isUsed() { return used; }
}
