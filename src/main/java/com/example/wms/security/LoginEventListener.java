package com.example.wms.security;

import com.example.wms.service.NotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * OBSERVER PATTERN
 * ----------------
 * Spring Security publishes an AuthenticationSuccessEvent for every successful
 * login — form-based AND OAuth2/OIDC (Google) alike, since both go through
 * Spring's authentication machinery. This class just subscribes to that one
 * event; it doesn't know or care which login method fired it.
 *
 * That's what makes this the right hook for "notify + log on every login,
 * regardless of method" — instead of duplicating that logic inside both
 * CustomUserDetailsService (form) and CustomOidcUserService (Google), it's
 * written once here and triggered by whichever one actually ran.
 */
@Component
public class LoginEventListener {

    private final NotificationService notificationService;

    public LoginEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        notificationService.notifyLogin(username);
    }
}
