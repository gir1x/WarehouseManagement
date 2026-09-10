package com.example.wms.service;

import com.example.wms.domain.AuthProvider;
import com.example.wms.domain.Role;
import com.example.wms.domain.User;
import com.example.wms.exception.ValidationException;
import com.example.wms.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Registration always produces a PENDING account (never VISITOR/ADMIN), and
 * rejects duplicate usernames/emails before hitting the database's own
 * unique constraint — see documentation §7 and the README's "accounts,
 * registration, and the approval flow" section.
 */
class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final AuthService authService = new AuthService(userRepository, passwordEncoder, notificationService);

    @Test
    void newAccountsAlwaysStartPending() {
        when(userRepository.findByUsername("priya")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("priya@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = authService.register("priya", "priya@example.com", "secret123");

        assertThat(created.getRole()).isEqualTo(Role.PENDING);
        assertThat(created.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(created.getPassword()).isEqualTo("hashed");
    }

    @Test
    void notifiesOnSuccessfulRegistration() {
        when(userRepository.findByUsername("priya")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("priya@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = authService.register("priya", "priya@example.com", "secret123");

        verify(notificationService).notifyAccountCreated(created);
    }

    @Test
    void rejectsDuplicateUsername() {
        when(userRepository.findByUsername("priya"))
                .thenReturn(Optional.of(new User("priya", "other@example.com", "x", Role.VISITOR, AuthProvider.LOCAL)));

        assertThatThrownBy(() -> authService.register("priya", "new@example.com", "secret123"))
                .isInstanceOf(ValidationException.class);

        verify(userRepository, never()).save(any());
        verify(notificationService, never()).notifyAccountCreated(any());
    }

    @Test
    void rejectsDuplicateEmail() {
        when(userRepository.findByUsername("newname")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("taken@example.com"))
                .thenReturn(Optional.of(new User("someoneelse", "taken@example.com", "x", Role.VISITOR, AuthProvider.LOCAL)));

        assertThatThrownBy(() -> authService.register("newname", "taken@example.com", "secret123"))
                .isInstanceOf(ValidationException.class);

        verify(userRepository, never()).save(any());
        verify(notificationService, never()).notifyAccountCreated(any());
    }
}
