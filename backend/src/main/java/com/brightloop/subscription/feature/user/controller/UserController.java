package com.brightloop.subscription.feature.user.controller;

import com.brightloop.subscription.common.dto.PageResponse;
import com.brightloop.subscription.common.enums.Role;
import com.brightloop.subscription.feature.user.dto.CreateStaffRequest;
import com.brightloop.subscription.feature.user.dto.UpdateRoleRequest;
import com.brightloop.subscription.feature.user.dto.UpdateUserRequest;
import com.brightloop.subscription.feature.user.dto.UserResponse;
import com.brightloop.subscription.feature.user.service.UserService;
import com.brightloop.subscription.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * UserController — ADMIN-only user management endpoints.
 *
 * @PreAuthorize("hasRole('ADMIN')") — evaluated by Spring's AOP proxy BEFORE
 * the method body runs (requires @EnableMethodSecurity in SecurityConfig).
 * Uses the "ROLE_" prefix convention established in CustomUserDetails.
 *
 * Pageable params: ?page=0&size=20&sort=fullName,asc
 * The Pageable object is resolved automatically by Spring MVC from query params.
 *
 * @AuthenticationPrincipal injects the CustomUserDetails set by JwtAuthFilter,
 * allowing us to pass the requesting admin's ID to the service for self-protection.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Admin-only user and staff management")
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users — paginated list with optional filters.
     * Query params: ?role=ADMIN&search=john&page=0&size=20&sort=fullName,asc
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users (paginated, filterable by role and search term)")
    public PageResponse<UserResponse> getAllUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return userService.getAllUsers(role, search, pageable);
    }

    /**
     * GET /api/users/{id} — fetch a single user by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get a user by ID")
    public UserResponse getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    /**
     * POST /api/users — create an INTERNAL or ADMIN staff account.
     * Cannot create CUSTOMER accounts here — use /api/auth/signup.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new staff account (INTERNAL or ADMIN role only)")
    public UserResponse createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return userService.createStaff(request);
    }

    /**
     * PUT /api/users/{id} — update fullName and active status.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a user's name and active status")
    public UserResponse updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return userService.updateUser(id, request);
    }

    /**
     * PATCH /api/users/{id}/role — change a user's role.
     * Admins cannot change their own role (enforced in service).
     */
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a user's role (admin cannot change own role)")
    public UserResponse updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return userService.updateRole(id, request, currentUser.getUserId());
    }
}
