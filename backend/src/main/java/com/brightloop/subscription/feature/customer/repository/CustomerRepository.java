package com.brightloop.subscription.feature.customer.repository;

import com.brightloop.subscription.feature.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for Customer entities.
 *
 * Includes paginated search with optional filters for name/email/company
 * and an active status filter. All filter params nullable to allow flexible
 * query combinations from the controller layer.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByEmail(String email);

    Optional<Customer> findByEmail(String email);

    /**
     * Finds a Customer linked to a specific User (for GET /api/customers/me).
     * Returns Optional.empty() if the user has no customer profile.
     */
    Optional<Customer> findByUserId(Long userId);

    /**
     * Paginated search with optional text and active-status filters.
     * Searches across name, email, and company_name fields.
     */
    @Query("""
            SELECT c FROM Customer c
            WHERE (:active IS NULL OR c.active = :active)
              AND (:search IS NULL
                   OR LOWER(c.name)        LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.email)       LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Customer> findAllWithFilters(
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable
    );
}
