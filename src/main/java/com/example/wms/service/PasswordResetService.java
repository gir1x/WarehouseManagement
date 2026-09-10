package com.example.wms.service;

import com.example.wms.domain.AuthProvider;
import com.example.wms.domain.PasswordResetToken;
import com.example.wms.domain.User;
import com.example.wms.exception.ValidationException;
import com.example.wms.repository.PasswordResetTokenRepository;
import com.example.wms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Duration TOKEN_VALIDITY = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final String baseUrl;

    public PasswordResetService(UserRepository userRepository,
                                 PasswordResetTokenRepository tokenRepository,
                                 PasswordEncoder passwordEncoder,
                                 MailService mailService,
                                 @Value("${app.base-url}") String baseUrl) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        // Read from config instead of hardcoded, because "what URL is this app
        // reachable at" changes per environment — localhost while developing,
        // a Cloudflare tunnel URL when you're testing through one, a real
        // domain in production. See app.base-url in application.yml.
        this.baseUrl = baseUrl;
    }

    /**
     * No @PreAuthorize — this is reachable by anyone who isn't signed in yet,
     * by definition. Deliberately does nothing observable different whether
     * the email exists, belongs to a Google-only account, or is a genuine
     * LOCAL account — the controller always returns the same generic
     * response either way. This is standard practice: it stops an attacker
     * from using "forgot password" to discover which emails are registered.
     */
    @Transactional
    public void requestReset(String email) {
        Optional<User> maybeUser = userRepository.findByEmail(email);
        if (maybeUser.isEmpty()) {
            return;
        }

        User user = maybeUser.get();
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            // A Google-only account has no local password to reset — nothing to do.
            // (Still returns normally so the caller can't tell the difference.)
            return;
        }

        String token = UUID.randomUUID().toString();
        tokenRepository.save(new PasswordResetToken(user, token, Instant.now().plus(TOKEN_VALIDITY)));

        String resetLink = baseUrl + "/reset-password?token=" + token;
        mailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new ValidationException("This reset link is invalid or has expired."));

        if (!resetToken.isUsable()) {
            throw new ValidationException("This reset link is invalid or has expired.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.markUsed();
        tokenRepository.save(resetToken);
    }
}
