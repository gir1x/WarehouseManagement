package com.example.wms.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true)
    private String email;

    /** BCrypt hash. Null for accounts that only ever sign in via OAuth2. */
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider;

    protected User() {
        // JPA only
    }

    public User(String username, String email, String password, Role role, AuthProvider authProvider) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.authProvider = authProvider;
    }

    public UUID getId() { return id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public AuthProvider getAuthProvider() { return authProvider; }
    public void setAuthProvider(AuthProvider authProvider) { this.authProvider = authProvider; }
}
