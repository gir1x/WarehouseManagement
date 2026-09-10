package com.example.wms.service;

import com.example.wms.domain.LoginActivity;
import com.example.wms.domain.Role;
import com.example.wms.domain.User;
import com.example.wms.repository.LoginActivityRepository;
import com.example.wms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private static final DateTimeFormatter IST_FORMAT =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                    .withLocale(Locale.ENGLISH)
                    .withZone(ZoneId.of("Asia/Kolkata"));

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
        try {
            if (newUser.getEmail() != null) {
                mailService.sendNotification(
                        newUser.getEmail(),
                        "Welcome to WMS",
                        "Your account (" + newUser.getUsername() + ") has been created.\n\n"
                                + "An admin needs to approve access before you can sign in and use the system — "
                                + "you'll be able to sign in once that happens."
                );
            }
        } catch (Exception e) {
            log.error("Failed to send account-created email to {}", newUser.getEmail(), e);
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
    public void recordLogin(String username) {
        loginActivityRepository.save(new LoginActivity(username, Instant.now()));
    }

    public void notifyLogin(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            log.warn("notifyLogin called for unknown username: {}", username);
            return;
        }

        String when = IST_FORMAT.format(Instant.now()) + " IST";

        try {
            if (user.getEmail() != null) {
                mailService.sendNotification(
                        user.getEmail(),
                        "New sign-in to your WMS account",
                        "Your account (" + username + ") just signed in at " + when + ".\n\n"
                                + "If this wasn't you, contact an admin."
                );
            }
        } catch (Exception e) {
            log.error("Failed to send login email to {}", user.getEmail(), e);
        }

        notifyAllAdmins("Sign-in: " + username, username + " signed in at " + when + ".");
    }

    private void notifyAllAdmins(String subject, String body) {
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        for (User admin : admins) {
            if (admin.getEmail() != null) {
                try {
                    mailService.sendNotification(admin.getEmail(), subject, body);
                } catch (Exception e) {
                    log.error("Failed to send admin notification to {}", admin.getEmail(), e);
                }
            }
        }
    }
}   
