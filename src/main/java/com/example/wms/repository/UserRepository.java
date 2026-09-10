package com.example.wms.repository;

import com.example.wms.domain.Role;
import com.example.wms.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    // Spring Data derives the query from the method name — "WHERE username = ?1 OR email = ?2".
    // Used by CustomUserDetailsService so the login form accepts either one in the same field.
    Optional<User> findByUsernameOrEmail(String username, String email);

    // Used by NotificationService to find everyone who should get an admin alert email.
    List<User> findByRole(Role role);
}
