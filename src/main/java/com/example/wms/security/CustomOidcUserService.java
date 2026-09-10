package com.example.wms.security;

import com.example.wms.domain.AuthProvider;
import com.example.wms.domain.Role;
import com.example.wms.domain.User;
import com.example.wms.repository.UserRepository;
import com.example.wms.service.NotificationService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Google's login is OpenID Connect, not plain OAuth2 — our scope includes
 * "openid", which is what makes it OIDC. Spring Security routes OIDC
 * providers through a DIFFERENT hook (.oidcUserService(...) in
 * SecurityConfig) than generic OAuth2 providers (.userService(...)).
 *
 * An earlier version of this class extended DefaultOAuth2UserService and
 * was wired via .userService(...) — for an OIDC provider like Google, that
 * hook is simply never called. Spring Security silently falls back to its
 * own built-in OidcUserService instead, which knows nothing about our User
 * table: no local row gets created, the authenticated principal's name
 * becomes Google's raw numeric "sub" claim instead of the email, and the
 * only granted authority is the generic "OIDC_USER" — which is why every
 * hasAnyRole("ADMIN","VISITOR") check then returned 403.
 *
 * Extending OidcUserService (not DefaultOAuth2UserService) and wiring it
 * via .oidcUserService(...) is what actually puts this class in the loop.
 *
 * New Google sign-ins are created as VISITOR (not PENDING like self-service
 * registration) — Google-verified identity gets immediate baseline access;
 * an admin can still promote or demote the account afterward from
 * /admin/users exactly like any other user.
 *
 * The "account created" notification only fires the first time a given
 * Google email signs in — every login after that (including this same one,
 * via LoginEventListener) still triggers the separate "login" notification.
 */
@Service
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public CustomOidcUserService(UserRepository userRepository, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        String email = oidcUser.getEmail(); // standard OIDC claim, available because scope includes "email"

        Optional<User> existing = userRepository.findByEmail(email);
        User user;
        if (existing.isPresent()) {
            user = existing.get();
        } else {
            user = userRepository.save(new User(email, email, null, Role.VISITOR, AuthProvider.GOOGLE));
            notificationService.notifyAccountCreated(user);
        }

        return new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                "email" // Authentication.getName() now returns the email, matching local-login users
        );
    }
}
