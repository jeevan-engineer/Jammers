package com.brightloop.subscription.feature.user.repository;

import com.brightloop.subscription.common.enums.Role;
import com.brightloop.subscription.feature.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for User entities.
 *
 * JpaRepository<User, Long> provides standard CRUD + pagination out of the box.
 * No boilerplate SQL needed for simple lookups.
 *
 * @Query uses JPQL (not native SQL) so it is database-agnostic and Hibernate
 * validates the entity paths at startup.
 *
 * Common error: using column names (e.g. full_name) instead of entity field
 * names (u.fullName) in JPQL will cause a QueryException at startup.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Paginated search with optional role and text filters.
     * LOWER() ensures case-insensitive matching without a DB collation dependency.
     * :role = null check allows callers to omit the role filter entirely.
     */
    @Query("""
            SELECT u FROM User u
            WHERE (:role IS NULL OR u.role = :role)
              AND (:search IS NULL
                   OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<User> findAllWithFilters(
            @Param("role") Role role,
            @Param("search") String search,
            Pageable pageable
    );
}
