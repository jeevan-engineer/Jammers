package com.brightloop.subscription.feature.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for POST /api/auth/login.
 * Intentionally has no password pattern validation here — the auth
 * layer will return BadCredentialsException for wrong passwords,
 * not a validation error.
 */
public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Entered email is invalid")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {}
