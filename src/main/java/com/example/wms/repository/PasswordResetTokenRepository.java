package com.example.wms.repository;

import com.example.wms.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByToken(String token);

    // Called before deleting a User — PasswordResetToken has a non-nullable FK to User
    // (see PasswordResetToken.user), so any leftover tokens have to go first or the
    // delete would fail on a foreign-key constraint violation.
    void deleteByUser_Id(UUID userId);
}
