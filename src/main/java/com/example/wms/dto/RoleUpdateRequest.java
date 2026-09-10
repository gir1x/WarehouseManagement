package com.example.wms.dto;

import com.example.wms.domain.Role;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(@NotNull Role role) {}
