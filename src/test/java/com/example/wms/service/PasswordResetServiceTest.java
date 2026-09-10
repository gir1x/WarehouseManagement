package com.example.wms.service;

import com.example.wms.domain.AuthProvider;
import com.example.wms.domain.PasswordResetToken;
import com.example.wms.domain.Role;
import com.example.wms.domain.User;
import com.example.wms.exception.ValidationException;
import com.example.wms.repository.PasswordResetTokenRepository;
import com.example.wms.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordResetTokenRepository tokenRepository = mock(PasswordResetTokenRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final MailService mailService = mock(MailService.class);
    private final PasswordResetService service =
            new PasswordResetService(userRepository, tokenRepository, passwordEncoder, mailService, "http://localhost:8080");

    @Test
    void requestingResetForALocalAccountSendsAnEmailAndSavesAToken() {
        User user = new User("priya", "priya@example.com", "hash", Role.VISITOR, AuthProvider.LOCAL);
        when(userRepository.findByEmail("priya@example.com")).thenReturn(Optional.of(user));

        service.requestReset("priya@example.com");

        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(mailService).sendPasswordResetEmail(eq("priya@example.com"), anyString());
    }

    @Test
    void requestingResetForAGoogleOnlyAccountDoesNothingObservable() {
        User user = new User("priya@example.com", "priya@example.com", null, Role.VISITOR, AuthProvider.GOOGLE);
        when(userRepository.findByEmail("priya@example.com")).thenReturn(Optional.of(user));

        service.requestReset("priya@example.com");

        verify(tokenRepository, never()).save(any());
        verify(mailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void requestingResetForAnUnknownEmailDoesNothingObservable() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        service.requestReset("nobody@example.com");

        verify(tokenRepository, never()).save(any());
        verify(mailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void resettingWithAValidTokenUpdatesThePassword() {
        User user = new User("priya", "priya@example.com", "oldHash", Role.VISITOR, AuthProvider.LOCAL);
        PasswordResetToken token = new PasswordResetToken(user, "abc123", Instant.now().plusSeconds(60));
        when(tokenRepository.findByToken("abc123")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newSecret1")).thenReturn("newHash");

        service.resetPassword("abc123", "newSecret1");

        assertThat(user.getPassword()).isEqualTo("newHash");
        assertThat(token.isUsed()).isTrue();
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    void resettingWithAnExpiredTokenThrows() {
        User user = new User("priya", "priya@example.com", "oldHash", Role.VISITOR, AuthProvider.LOCAL);
        PasswordResetToken expired = new PasswordResetToken(user, "abc123", Instant.now().minusSeconds(60));
        when(tokenRepository.findByToken("abc123")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.resetPassword("abc123", "newSecret1"))
                .isInstanceOf(ValidationException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void resettingWithAnAlreadyUsedTokenThrows() {
        User user = new User("priya", "priya@example.com", "oldHash", Role.VISITOR, AuthProvider.LOCAL);
        PasswordResetToken used = new PasswordResetToken(user, "abc123", Instant.now().plusSeconds(60));
        used.markUsed();
        when(tokenRepository.findByToken("abc123")).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> service.resetPassword("abc123", "newSecret1"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void resettingWithAnUnknownTokenThrows() {
        when(tokenRepository.findByToken("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("nope", "newSecret1"))
                .isInstanceOf(ValidationException.class);
    }
}
