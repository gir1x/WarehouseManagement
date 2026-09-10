package com.example.wms.service;

import com.example.wms.domain.LoginActivity;
import com.example.wms.domain.Role;
import com.example.wms.domain.User;
import com.example.wms.repository.LoginActivityRepository;
import com.example.wms.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

/**
 * Decides WHAT to email and WHO to email it to; MailService only knows HOW
 * to actually send. Two events trigger this:
 *  - a new account is created (AuthService.register(), or a first-time
 *    Google sign-in in CustomOidcUserService)
 *  - a successful login (LoginEventListener, below)
 *
 * Every admin gets a copy of both — that's what was asked for, even though
 * in a real product you'd likely throttle "every single login" down to a
 * daily digest or an anomaly-only alert before it reached real users.
 */
@Service
public class NotificationService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.ENGLISH).withZone(java.time.ZoneOffset.UTC);

    private final MailService mailService;
    private final UserRepository userRepository;
    private final LoginActivityRepository loginActivityRepository;

    public NotificationService(MailService mailService,
                                UserRepository userRepository,
                                LoginActivityRepository loginActivityRepository) {
        this.mailService = mailService;
        this.userRepository = userRepository;
        this.loginActivityRepository = loginActivityRepository;
    }

    public void notifyAccountCreated(User newUser) {
        if (newUser.getEmail() != null) {
            mailService.sendNotification(
                    newUser.getEmail(),
                    "Welcome to WMS",
                    "Your account (" + newUser.getUsername() + ") has been created.\n\n"
                            + "An admin needs to approve access before you can sign in and use the system — "
                            + "you'll be able to sign in once that happens."
            );
        }

        notifyAllAdmins(
                "New account: " + newUser.getUsername(),
                "A new account was just created and is awaiting approval.\n\n"
                        + "Username: " + newUser.getUsername() + "\n"
                        + "Email: " + newUser.getEmail() + "\n"
                        + "Signed up via: " + newUser.getAuthProvider() + "\n\n"
                        + "Visit /admin/users to review and assign a role."
        );
    }

    @Transactional
    public void notifyLogin(String username) {
        loginActivityRepository.save(new LoginActivity(username, Instant.now()));

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            // Shouldn't happen for a successful authentication, but notification
            // failing here should never be what breaks someone's login.
            return;
        }

        // String when = TIMESTAMP_FORMAT.format(Instant.now()) + " UTC";
        DateTimeFormatter istFormatter = TIMESTAMP_FORMAT.withZone(ZoneId.of("Asia/Kolkata"));

        String when = istFormatter.format(Instant.now()) + " IST";

        if (user.getEmail() != null) {
            mailService.sendNotification(
                    user.getEmail(),
                    "New sign-in to your WMS account",
                    "Your account (" + username + ") just signed in at " + when + ".\n\n"
                            + "If this wasn't you, contact an admin."
            );
        }

        notifyAllAdmins(
                "Sign-in: " + username,
                username + " signed in at " + when + "."
        );
    }

    private void notifyAllAdmins(String subject, String body) {
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        for (User admin : admins) {
            if (admin.getEmail() != null) {
                mailService.sendNotification(admin.getEmail(), subject, body);
            }
        }
    }
}
