package com.example.wms.service;

import com.example.wms.domain.AuthProvider;
import com.example.wms.domain.Role;
import com.example.wms.domain.User;
import com.example.wms.exception.ValidationException;
import com.example.wms.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Self-service account creation. No @PreAuthorize here on purpose — a person
 * registering doesn't have a session yet, so there's nothing to authorize
 * against. The new account is created with Role.PENDING and can't do
 * anything until an ADMIN promotes it (see UserAdminService).
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public AuthService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        NotificationService notificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    @Transactional
    public User register(String username, String email, String rawPassword) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new ValidationException("That username is already taken.");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ValidationException("An account with that email already exists.");
        }

        User user = new User(username, email, passwordEncoder.encode(rawPassword), Role.PENDING, AuthProvider.LOCAL);
        User saved = userRepository.save(user);

        notificationService.notifyAccountCreated(saved);

        return saved;
    }
}
