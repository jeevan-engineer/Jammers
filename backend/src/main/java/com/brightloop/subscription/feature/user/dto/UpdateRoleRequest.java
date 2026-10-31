package com.brightloop.subscription.feature.user.dto;

import com.brightloop.subscription.common.enums.Role;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for PATCH /api/users/{id}/role — role assignment by ADMIN.
 */
public record UpdateRoleRequest(
        @NotNull(message = "Role is required")
        Role role
) {}
