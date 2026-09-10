package com.example.wms.config;

import com.example.wms.domain.AuthProvider;
import com.example.wms.domain.Product;
import com.example.wms.domain.Role;
import com.example.wms.domain.User;
import com.example.wms.repository.ProductRepository;
import com.example.wms.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a default ADMIN, VISITOR, and PENDING account plus one sample
 * product on every startup, so you have something to log in and test with
 * immediately — including the approval flow, without having to register a
 * new account by hand first.
 *
 * Runs against whatever database is configured in application.yml. With
 * ddl-auto: update, re-running against the same Postgres database is safe —
 * each block checks whether the row already exists before inserting.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                       ProductRepository productRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        userRepository.findByUsername("admin").ifPresentOrElse(
                u -> {},
                () -> userRepository.save(new User(
                        "admin", "admin@example.com",
                        passwordEncoder.encode("admin123"),
                        Role.ADMIN, AuthProvider.LOCAL
                ))
        );

        userRepository.findByUsername("visitor").ifPresentOrElse(
                u -> {},
                () -> userRepository.save(new User(
                        "visitor", "visitor@example.com",
                        passwordEncoder.encode("visitor123"),
                        Role.VISITOR, AuthProvider.LOCAL
                ))
        );

        // Demonstrates the approval flow out of the box: sign in as "admin",
        // go to Manage Users, and promote this account to see it unlock.
        userRepository.findByUsername("newuser").ifPresentOrElse(
                u -> {},
                () -> userRepository.save(new User(
                        "newuser", "newuser@example.com",
                        passwordEncoder.encode("newuser123"),
                        Role.PENDING, AuthProvider.LOCAL
                ))
        );

        productRepository.findBySku("SKU-001").ifPresentOrElse(
                p -> {},
                () -> productRepository.save(new Product("SKU-001", "Sample Product"))
        );
    }
}
