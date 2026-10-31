package com.brightloop.subscription.feature.auth.dto;

/**
 * Response DTO returned after successful signup or login.
 *
 * Fields:
 *  - token      : the signed JWT string (client must store this securely)
 *  - tokenType  : always "Bearer" — how the token should be sent in headers
 *  - expiresIn  : token TTL in milliseconds (from app.jwt.expiration-ms)
 *  - userId     : the authenticated user's database ID
 *  - fullName   : display name for the UI
 *  - email      : the authenticated user's email
 *  - role       : the user's role (ADMIN / INTERNAL / CUSTOMER)
 *
 * passwordHash is intentionally NEVER included here.
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        Long userId,
        String fullName,
        String email,
        String role
) {
    /** Factory for a standard Bearer auth response. */
    public static AuthResponse bearer(String token, long expiresIn,
                                      Long userId, String fullName,
                                      String email, String role) {
        return new AuthResponse(token, "Bearer", expiresIn, userId, fullName, email, role);
    }
}
