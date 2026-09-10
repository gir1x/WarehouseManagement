package com.example.wms.controller;

import com.example.wms.domain.User;
import com.example.wms.dto.RoleUpdateRequest;
import com.example.wms.dto.UserResponse;
import com.example.wms.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** ADMIN only — enforced at the URL level ("/api/admin/**" in SecurityConfig) and again in UserAdminService. */
@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> list() {
        List<UserResponse> users = userAdminService.listUsers().stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<UserResponse> updateRole(@PathVariable UUID id,
                                                     @Valid @RequestBody RoleUpdateRequest request,
                                                     Authentication authentication) {
        User updated = userAdminService.updateRole(id, request.role(), authentication.getName());
        return ResponseEntity.ok(UserResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Authentication authentication) {
        userAdminService.deleteUser(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
