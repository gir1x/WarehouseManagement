package com.example.wms.service;

import com.example.wms.domain.Role;
import com.example.wms.domain.User;
import com.example.wms.exception.UserNotFoundException;
import com.example.wms.exception.ValidationException;
import com.example.wms.repository.PasswordResetTokenRepository;
import com.example.wms.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Lets an ADMIN see every account, grant/change roles, and remove accounts entirely. */
@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public UserAdminService(UserRepository userRepository,
                             PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public User updateRole(UUID targetUserId, Role newRole, String actingUsername) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + targetUserId));

        // Compared by username, not id — usernames are unique and known up front
        // (from the Authentication principal), so this needs no extra lookup of
        // the acting admin's own row. Also just makes this rule simpler to unit
        // test: no need for a persisted entity with a JPA-assigned id.
        if (target.getUsername().equals(actingUsername)) {
            // Prevents an admin from locking themselves out (or everyone else,
            // if they're the only admin) by accident.
            throw new ValidationException("You can't change your own role. Ask another admin.");
        }

        target.setRole(newRole);
        return userRepository.save(target);
    }

    /**
     * Same self-protection as updateRole — an admin can't delete their own
     * account here, for the same "don't lock yourself out" reason.
     * StockMovement.performedBy is a plain String snapshot (not a foreign
     * key), so deleting a user never breaks the audit trail — old movement
     * rows just keep the username as history. PasswordResetToken DOES have a
     * real foreign key to User, so any leftover tokens are cleaned up first.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteUser(UUID targetUserId, String actingUsername) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + targetUserId));

        if (target.getUsername().equals(actingUsername)) {
            throw new ValidationException("You can't delete your own account. Ask another admin.");
        }

        passwordResetTokenRepository.deleteByUser_Id(targetUserId);
        userRepository.delete(target);
    }
}
