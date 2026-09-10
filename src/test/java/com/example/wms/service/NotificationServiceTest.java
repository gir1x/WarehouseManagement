package com.example.wms.service;

import com.example.wms.domain.AuthProvider;
import com.example.wms.domain.LoginActivity;
import com.example.wms.domain.Role;
import com.example.wms.domain.User;
import com.example.wms.repository.LoginActivityRepository;
import com.example.wms.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Decides WHAT gets emailed and WHO gets it — MailService itself is mocked
 * out here since sending is its own separately-tested concern (see
 * MailServiceTest).
 */
class NotificationServiceTest {

    private final MailService mailService = mock(MailService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final LoginActivityRepository loginActivityRepository = mock(LoginActivityRepository.class);
    private final NotificationService notificationService =
            new NotificationService(mailService, userRepository, loginActivityRepository);

    @Test
    void newAccountEmailsTheNewUserAndEveryAdmin() {
        User newUser = new User("priya", "priya@example.com", "hash", Role.PENDING, AuthProvider.LOCAL);
        User admin1 = new User("admin1", "admin1@example.com", "hash", Role.ADMIN, AuthProvider.LOCAL);
        User admin2 = new User("admin2", "admin2@example.com", "hash", Role.ADMIN, AuthProvider.LOCAL);
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(admin1, admin2));

        notificationService.notifyAccountCreated(newUser);

        verify(mailService).sendNotification(eq("priya@example.com"), anyString(), anyString());
        verify(mailService).sendNotification(eq("admin1@example.com"), anyString(), anyString());
        verify(mailService).sendNotification(eq("admin2@example.com"), anyString(), anyString());
    }

    @Test
    void loginLogsTheActivityAndEmailsTheUserAndAdmins() {
        User user = new User("priya", "priya@example.com", "hash", Role.VISITOR, AuthProvider.LOCAL);
        User admin = new User("admin", "admin@example.com", "hash", Role.ADMIN, AuthProvider.LOCAL);
        when(userRepository.findByUsername("priya")).thenReturn(Optional.of(user));
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(admin));

        notificationService.notifyLogin("priya");

        verify(loginActivityRepository).save(any(LoginActivity.class));
        verify(mailService).sendNotification(eq("priya@example.com"), anyString(), anyString());
        verify(mailService).sendNotification(eq("admin@example.com"), anyString(), anyString());
    }

    @Test
    void loginForAnUnknownUsernameStillLogsButSendsNoEmail() {
        // Shouldn't happen for a real successful authentication, but the
        // event listener shouldn't ever throw over it either way.
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        notificationService.notifyLogin("ghost");

        verify(loginActivityRepository).save(any(LoginActivity.class));
        verify(mailService, never()).sendNotification(anyString(), anyString(), anyString());
    }
}
