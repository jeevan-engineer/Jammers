package com.brightloop.subscription.feature.auth.controller;

import com.brightloop.subscription.feature.auth.dto.AuthResponse;
import com.brightloop.subscription.feature.auth.dto.LoginRequest;
import com.brightloop.subscription.feature.auth.dto.SignupRequest;
import com.brightloop.subscription.feature.auth.service.AuthService;
import com.brightloop.subscription.feature.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Auth controller — handles signup, login, /me, and logout.
 *
 * @RestController = @Controller + @ResponseBody; all return values are
 * serialised directly to the HTTP response body as JSON.
 *
 * @RequiredArgsConstructor generates a constructor injecting all final fields —
 * the team standard for constructor injection.
 *
 * @Valid on @RequestBody triggers bean validation (MethodArgumentNotValidException
 * on failure, handled by GlobalExceptionHandler → 400 with field errors).
 *
 * POST /api/auth/logout — JWT is stateless so the server cannot invalidate it.
 * The client is responsible for deleting the token from storage.
 * This endpoint simply confirms receipt and documents the expected behavior.
 * For true server-side invalidation, a token blacklist (Redis/DB) would be
 * needed — out of scope for this hackathon.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Signup, login, current user, and logout")
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/signup — public.
     * Creates a CUSTOMER user + linked customer profile atomically.
     * Returns 201 Created with a JWT.
     */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new customer account",
               security = @SecurityRequirement(name = ""))
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    /**
     * POST /api/auth/login — public.
     * Authenticates and returns a JWT.
     */
    @PostMapping("/login")
    @Operation(summary = "Login and get a JWT token",
               security = @SecurityRequirement(name = ""))
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * GET /api/auth/me — requires valid JWT.
     * Returns the profile of the currently authenticated user.
     *
     * @AuthenticationPrincipal injects the UserDetails set by JwtAuthFilter.
     */
    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public UserResponse getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return authService.getCurrentUser(userDetails.getUsername());
    }

    /**
     * POST /api/auth/logout — stateless logout.
     *
     * IMPORTANT: This endpoint does NOT invalidate the JWT server-side.
     * The client MUST delete the token from localStorage/sessionStorage/cookie.
     * A future enhancement could add a Redis-backed token blacklist if needed.
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout (client must delete the token)")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of(
                "message", "Logged out successfully. Please delete your token."
        ));
    }
}
