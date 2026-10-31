package com.brightloop.subscription.feature.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for POST /api/auth/signup.
 *
 * @Email — validates format; custom message matches the spec exactly.
 * @Pattern — enforces the password policy in one regex:
 *   (?=.*[A-Z])   at least one uppercase letter
 *   (?=.*[a-z])   at least one lowercase letter
 *   (?=.*[^A-Za-z0-9]) at least one special character (non-alphanumeric)
 *   .{9,}         MORE than 8 characters (i.e., minimum 9)
 *
 * Using Java records keeps the DTO immutable and concise.
 */
public record SignupRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Entered email is invalid")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[^A-Za-z0-9]).{9,}$",
                message = "Password must be more than 8 characters and contain " +
                          "at least one uppercase letter, one lowercase letter, " +
                          "and one special character"
        )
        String password
) {}
