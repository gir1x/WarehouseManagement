package com.example.wms.security;

import com.example.wms.domain.AuthProvider;
import com.example.wms.domain.Role;
import com.example.wms.domain.User;
import com.example.wms.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The login form's single field accepts either a username or an email — see §8.3 of the study notes. */
class CustomUserDetailsServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

    @Test
    void loadsAUserByUsername() {
        User user = new User("priya", "priya@example.com", "hash", Role.VISITOR, AuthProvider.LOCAL);
        when(userRepository.findByUsernameOrEmail("priya", "priya")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("priya");

        assertThat(result.getUsername()).isEqualTo("priya");
        assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_VISITOR");
    }

    @Test
    void loadsAUserByEmailToo() {
        User user = new User("priya", "priya@example.com", "hash", Role.VISITOR, AuthProvider.LOCAL);
        when(userRepository.findByUsernameOrEmail("priya@example.com", "priya@example.com"))
                .thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("priya@example.com");

        // Regardless of which field was typed to log in, the principal's name is
        // always the real username — this is what keeps audit logs, Authentication
        // .getName(), and UserAdminService's self-change guard consistent.
        assertThat(result.getUsername()).isEqualTo("priya");
    }

    @Test
    void throwsWhenNeitherMatches() {
        when(userRepository.findByUsernameOrEmail("nobody", "nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nobody"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
