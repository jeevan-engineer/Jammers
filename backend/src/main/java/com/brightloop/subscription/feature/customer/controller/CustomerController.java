package com.brightloop.subscription.feature.customer.controller;

import com.brightloop.subscription.common.dto.PageResponse;
import com.brightloop.subscription.feature.customer.dto.CustomerRequest;
import com.brightloop.subscription.feature.customer.dto.CustomerResponse;
import com.brightloop.subscription.feature.customer.service.CustomerService;
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
 * CustomerController — customer profile management endpoints.
 *
 * Role matrix enforced by @PreAuthorize:
 *  GET  /api/customers        → ADMIN or INTERNAL
 *  POST /api/customers        → ADMIN or INTERNAL
 *  GET  /api/customers/{id}   → ADMIN, INTERNAL, or the owning CUSTOMER (ownership checked in service)
 *  PUT  /api/customers/{id}   → ADMIN, INTERNAL, or the owning CUSTOMER
 *  GET  /api/customers/me     → CUSTOMER only
 *
 * @PreAuthorize with hasAnyRole() uses the "ROLE_" prefix set by CustomUserDetails.
 * The OR expression allows multiple roles while still requiring authentication.
 *
 * Ownership for /api/customers/{id} GET and PUT is enforced in the SERVICE layer
 * (not just by @PreAuthorize) because @PreAuthorize can't check record-level ownership
 * without a SpEL call to the repository — simpler to put the logic in the service.
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "Customer profile operations")
public class CustomerController {

    private final CustomerService customerService;

    /**
     * GET /api/customers — paginated list for staff.
     * Query params: ?search=acme&active=true&page=0&size=20
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL')")
    @Operation(summary = "List all customers (paginated, filterable)")
    public PageResponse<CustomerResponse> getAllCustomers(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return customerService.getAllCustomers(active, search, pageable);
    }

    /**
     * POST /api/customers — create a customer without a login.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL')")
    @Operation(summary = "Create a customer profile (no login account)")
    public CustomerResponse createCustomer(@Valid @RequestBody CustomerRequest request) {
        return customerService.createCustomer(request);
    }

    /**
     * GET /api/customers/me — returns the CUSTOMER's own profile.
     * Must be declared BEFORE /{id} to prevent Spring from treating "me" as an ID.
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get my own customer profile (CUSTOMER role)")
    public CustomerResponse getMyProfile(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return customerService.getMyProfile(currentUser.getUserId());
    }

    /**
     * GET /api/customers/{id} — accessible by staff or the owning CUSTOMER.
     * Service enforces ownership for CUSTOMER role.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL', 'CUSTOMER')")
    @Operation(summary = "Get a customer by ID (CUSTOMER role restricted to own profile)")
    public CustomerResponse getCustomerById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return customerService.getCustomerById(id, currentUser);
    }

    /**
     * PUT /api/customers/{id} — update a customer profile.
     * Service enforces ownership for CUSTOMER role.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERNAL', 'CUSTOMER')")
    @Operation(summary = "Update a customer profile (CUSTOMER role restricted to own profile)")
    public CustomerResponse updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return customerService.updateCustomer(id, request, currentUser);
    }
}
