package com.brightloop.subscription.feature.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Shared request DTO for both POST and PUT /api/customers.
 *
 * Validation:
 * - name: required, max 150 chars (matches customers.name column).
 * - email: required, valid format, max 150 chars.
 * - phone: optional, but when provided must be 7–15 digit characters only.
 * - companyName: optional, max 150 chars.
 * - address: optional, max 255 chars.
 * - taxId: optional, max 50 chars.
 *
 * @Pattern on phone — \d{7,15} matches exactly 7 to 15 digits.
 * nullable = phone field may be null (omitted entirely) or a valid digit string.
 */
public record CustomerRequest(

        @NotBlank(message = "Customer name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Entered email is invalid")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @Pattern(regexp = "^\\d{7,15}$",
                 message = "Phone number must be between 7 and 15 digits")
        String phone,

        @Size(max = 150, message = "Company name must not exceed 150 characters")
        String companyName,

        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        @Size(max = 50, message = "Tax ID must not exceed 50 characters")
        String taxId
) {}
