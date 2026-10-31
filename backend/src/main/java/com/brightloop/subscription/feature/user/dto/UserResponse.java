package com.brightloop.subscription.feature.user.dto;

import com.brightloop.subscription.common.enums.Role;

import java.time.LocalDateTime;

/**
 * User response DTO — returned by all user-related endpoints.
 * passwordHash is intentionally absent. This is the only user representation
 * that leaves the service layer.
 */
public record UserResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
