package com.example.wms.dto;

import com.example.wms.domain.User;

import java.util.UUID;

public record UserResponse(UUID id, String username, String email, String role, String authProvider) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getAuthProvider().name()
        );
    }
}
