package com.brightloop.subscription.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT token generation and validation using jjwt 0.12.x.
 *
 * jjwt 0.12.x API differences from older versions:
 *  - Jwts.builder() and Jwts.parser() are still the entry points.
 *  - signWith(key) auto-selects the algorithm from the key type.
 *  - Keys.hmacShaKeyFor() derives an HS256/384/512 key from raw bytes.
 *  - Claims parsing: Jwts.parser().verifyWith(key).build().parseSignedClaims(token).
 *
 * Claims stored: sub=email, role=ROLE_NAME, userId=Long.
 *
 * Common errors:
 *  - Key too short: HS256 requires >= 32 bytes; use a long secret in properties.
 *  - SignatureException or MalformedJwtException: token tampered or wrong secret.
 *  - ExpiredJwtException: token past its expiry — return 401.
 */
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        // Keys.hmacShaKeyFor() validates key length and picks the correct HMAC algorithm.
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Generates a signed JWT with subject=email, plus role and userId claims.
     *
     * @param email    the user's email (becomes the JWT subject)
     * @param role     the user's role string (e.g. "ADMIN")
     * @param userId   the user's database ID
     */
    public String generateToken(String email, String role, Long userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("userId", userId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extracts the email (subject) from a valid JWT.
     * Throws JwtException if the token is invalid or expired.
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Validates the token: checks signature and expiry.
     *
     * @return true if valid, false if any JwtException is thrown.
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Returns token validity period in milliseconds (for AuthResponse). */
    public long getExpirationMs() {
        return expirationMs;
    }

    // ── Private helpers ──────────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
