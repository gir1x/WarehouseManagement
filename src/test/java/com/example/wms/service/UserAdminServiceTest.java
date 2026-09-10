package com.example.wms.service;

import com.example.wms.domain.AuthProvider;
import com.example.wms.domain.Role;
import com.example.wms.domain.User;
import com.example.wms.exception.UserNotFoundException;
import com.example.wms.exception.ValidationException;
import com.example.wms.repository.PasswordResetTokenRepository;
import com.example.wms.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * The "admin gives access" step (documentation §7, README's approval flow):
 * an ADMIN can promote or delete any other account, but not their own.
 */
class UserAdminServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordResetTokenRepository passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
    private final UserAdminService userAdminService =
            new UserAdminService(userRepository, passwordResetTokenRepository);

    @Test
    void promotesAPendingUserToVisitor() {
        UUID targetId = UUID.randomUUID();
        User target = new User("newuser", "newuser@example.com", "hash", Role.PENDING, AuthProvider.LOCAL);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        User result = userAdminService.updateRole(targetId, Role.VISITOR, "admin");

        assertThat(result.getRole()).isEqualTo(Role.VISITOR);
        verify(userRepository).save(target);
    }

    @Test
    void adminCannotChangeTheirOwnRole() {
        UUID adminId = UUID.randomUUID();
        User admin = new User("admin", "admin@example.com", "hash", Role.ADMIN, AuthProvider.LOCAL);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userAdminService.updateRole(adminId, Role.VISITOR, "admin"))
                .isInstanceOf(ValidationException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void throwsWhenTargetUserDoesNotExist() {
        UUID missingId = UUID.randomUUID();
        when(userRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAdminService.updateRole(missingId, Role.VISITOR, "admin"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void deletesAnotherUsersAccount() {
        UUID targetId = UUID.randomUUID();
        User target = new User("newuser", "newuser@example.com", "hash", Role.VISITOR, AuthProvider.LOCAL);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        userAdminService.deleteUser(targetId, "admin");

        // Reset tokens are cleaned up first, since PasswordResetToken has a
        // non-nullable foreign key to User — deleting the user first would
        // fail on that constraint if any tokens were left behind.
        verify(passwordResetTokenRepository).deleteByUser_Id(targetId);
        verify(userRepository).delete(target);
    }

    @Test
    void adminCannotDeleteTheirOwnAccount() {
        UUID adminId = UUID.randomUUID();
        User admin = new User("admin", "admin@example.com", "hash", Role.ADMIN, AuthProvider.LOCAL);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userAdminService.deleteUser(adminId, "admin"))
                .isInstanceOf(ValidationException.class);

        verify(userRepository, never()).delete(any());
        verify(passwordResetTokenRepository, never()).deleteByUser_Id(any());
    }

    @Test
    void throwsWhenDeletingAnUnknownUser() {
        UUID missingId = UUID.randomUUID();
        when(userRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAdminService.deleteUser(missingId, "admin"))
                .isInstanceOf(UserNotFoundException.class);
    }
}
