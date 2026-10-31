package com.brightloop.subscription.feature.customer.mapper;

import com.brightloop.subscription.feature.customer.dto.CustomerResponse;
import com.brightloop.subscription.feature.customer.entity.Customer;
import org.springframework.stereotype.Component;

/**
 * Manual mapper converting Customer entity to CustomerResponse DTO.
 *
 * Accessing customer.getUser() — the User relationship is LAZY-loaded.
 * This method must be called within an active Hibernate session (i.e., inside
 * a @Transactional method or a service call that is already transactional).
 * If called outside a session, a LazyInitializationException is thrown.
 *
 * linkedUserId is extracted safely: customer.getUser() may return null for
 * staff-created customers (user_id IS NULL in the DB).
 */
@Component
public class CustomerMapper {

    public CustomerResponse toResponse(Customer customer) {
        if (customer == null) return null;

        Long linkedUserId = (customer.getUser() != null)
                ? customer.getUser().getId()
                : null;

        return new CustomerResponse(
                customer.getId(),
                linkedUserId,
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getCompanyName(),
                customer.getAddress(),
                customer.getTaxId(),
                customer.getActive(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}
