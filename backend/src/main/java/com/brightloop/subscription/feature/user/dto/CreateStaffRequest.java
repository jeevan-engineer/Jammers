package com.brightloop.subscription.feature.user.dto;

import com.brightloop.subscription.common.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for POST /api/users (admin creating a staff account).
 *
 * Role must be ADMIN or INTERNAL — CUSTOMER accounts are created only
 * via /api/auth/signup. This constraint is enforced in UserServiceImpl.
 *
 * The password validation here applies the same policy as signup for consistency.
 */
public record CreateStaffRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Entered email is invalid")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
        String password,

        /**
         * Role for the new staff member. Defaults to INTERNAL if omitted.
         * CUSTOMER is rejected by the service layer with a BusinessRuleException.
         */
        Role role
) {}
