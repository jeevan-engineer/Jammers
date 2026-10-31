package com.brightloop.subscription.feature.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for PUT /api/users/{id} — update user name and active status.
 * Email and role have dedicated endpoints (role via PATCH /api/users/{id}/role).
 */
public record UpdateUserRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        @NotNull(message = "Active status is required")
        Boolean active
) {}
