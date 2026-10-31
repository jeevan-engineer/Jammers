package com.brightloop.subscription.feature.customer.service;

import com.brightloop.subscription.common.dto.PageResponse;
import com.brightloop.subscription.common.enums.Role;
import com.brightloop.subscription.common.exception.DuplicateResourceException;
import com.brightloop.subscription.common.exception.ResourceNotFoundException;
import com.brightloop.subscription.feature.customer.dto.CustomerRequest;
import com.brightloop.subscription.feature.customer.dto.CustomerResponse;
import com.brightloop.subscription.feature.customer.entity.Customer;
import com.brightloop.subscription.feature.customer.mapper.CustomerMapper;
import com.brightloop.subscription.feature.customer.repository.CustomerRepository;
import com.brightloop.subscription.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CustomerServiceImpl — customer profile management.
 *
 * Ownership check pattern (CUSTOMER role):
 *   A CUSTOMER can only GET or PUT their own profile.
 *   ADMIN and INTERNAL can access any customer's profile.
 *   The check compares customer.getUser().getId() with currentUser.getUserId().
 *   If mismatched, AccessDeniedException → 403 via GlobalExceptionHandler.
 *
 * LAZY loading: customer.getUser() is LAZY. Since all service methods are
 * @Transactional, the Hibernate session is open when the mapper calls
 * getUser().getId(), so no LazyInitializationException occurs.
 *
 * Common error: accessing LAZY associations outside a @Transactional boundary
 * (e.g., in a controller or filter) causes LazyInitializationException.
 */
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getAllCustomers(Boolean active, String search,
                                                         Pageable pageable) {
        Page<CustomerResponse> page = customerRepository
                .findAllWithFilters(active, search, pageable)
                .map(customerMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        if (customerRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Customer", "email", request.email());
        }

        Customer customer = Customer.builder()
                .user(null)          // Staff-created: no linked user account.
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .companyName(request.companyName())
                .address(request.address())
                .taxId(request.taxId())
                .active(true)
                .build();

        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id, CustomUserDetails currentUser) {
        Customer customer = findCustomerOrThrow(id);
        assertOwnershipOrStaff(customer, currentUser);
        return customerMapper.toResponse(customer);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerRequest request,
                                           CustomUserDetails currentUser) {
        Customer customer = findCustomerOrThrow(id);
        assertOwnershipOrStaff(customer, currentUser);

        // Check email uniqueness only if it is changing.
        if (!customer.getEmail().equalsIgnoreCase(request.email())
                && customerRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Customer", "email", request.email());
        }

        customer.setName(request.name());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setCompanyName(request.companyName());
        customer.setAddress(request.address());
        customer.setTaxId(request.taxId());
        // Dirty-checking: Hibernate will issue UPDATE at transaction commit.
        return customerMapper.toResponse(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getMyProfile(Long userId) {
        Customer customer = customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No customer profile found for the current user."));
        return customerMapper.toResponse(customer);
    }

    // ── Private helpers ──────────────────────────────────────────────

    private Customer findCustomerOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    /**
     * Ownership check: CUSTOMER role can only access their own profile.
     * ADMIN and INTERNAL have unrestricted access.
     * Throws AccessDeniedException (→ 403) if a CUSTOMER tries to access another's profile.
     */
    private void assertOwnershipOrStaff(Customer customer, CustomUserDetails currentUser) {
        if (currentUser.getRole() == Role.CUSTOMER) {
            boolean isOwner = customer.getUser() != null
                    && customer.getUser().getId().equals(currentUser.getUserId());
            if (!isOwner) {
                throw new AccessDeniedException(
                        "You can only access your own customer profile.");
            }
        }
    }
}
