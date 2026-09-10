package com.example.wms.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lets the frontend ask "who am I / what role do I have" so it can hide
 * admin-only controls (e.g. "Create warehouse") for a VISITOR. This is a
 * UX convenience only — the actual enforcement is the URL rules in
 * SecurityConfig and the @PreAuthorize checks in the service layer.
 */
@RestController
public class MeController {

    @GetMapping("/api/me")
    public MeResponse me(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .map(a -> a.replace("ROLE_", ""))
                .orElse("UNKNOWN");
        return new MeResponse(authentication.getName(), role);
    }

    public record MeResponse(String username, String role) {}
}
