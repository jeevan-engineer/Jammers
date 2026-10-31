package com.brightloop.subscription.feature.customer.service;

import com.brightloop.subscription.common.dto.PageResponse;
import com.brightloop.subscription.feature.customer.dto.CustomerRequest;
import com.brightloop.subscription.feature.customer.dto.CustomerResponse;
import com.brightloop.subscription.security.CustomUserDetails;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    PageResponse<CustomerResponse> getAllCustomers(Boolean active, String search, Pageable pageable);

    /**
     * Creates a customer without a linked user account (user_id = null).
     * Used by ADMIN/INTERNAL staff for customers who don't use the portal.
     */
    CustomerResponse createCustomer(CustomerRequest request);

    /**
     * Retrieves a customer by ID with ownership check.
     * @param currentUser the authenticated principal (for CUSTOMER role ownership check)
     */
    CustomerResponse getCustomerById(Long id, CustomUserDetails currentUser);

    /**
     * Updates a customer's profile.
     * @param currentUser used for ownership check (CUSTOMER can only update their own profile)
     */
    CustomerResponse updateCustomer(Long id, CustomerRequest request, CustomUserDetails currentUser);

    /**
     * Returns the customer profile linked to the currently authenticated CUSTOMER user.
     */
    CustomerResponse getMyProfile(Long userId);
}
