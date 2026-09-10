package com.example.wms.security;

import com.example.wms.domain.User;
import com.example.wms.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads local (form-login) users. Spring Security auto-wires this into the
 * AuthenticationManager.
 *
 * The login form's single "Username" field accepts either an actual username
 * or an email address — whatever the person typed is checked against both
 * columns in one query. Whichever one matched, UserDetails.getUsername()
 * below still returns the account's real username (not whatever was typed),
 * so everything downstream (Authentication.getName(), audit logs, the
 * self-role-change guard in UserAdminService) stays consistent regardless of
 * which field someone logged in with.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("No such user: " + usernameOrEmail));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .build();
    }
}
