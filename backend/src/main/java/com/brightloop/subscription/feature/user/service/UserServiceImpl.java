package com.brightloop.subscription.feature.user.service;

import com.brightloop.subscription.common.dto.PageResponse;
import com.brightloop.subscription.common.enums.Role;
import com.brightloop.subscription.common.exception.BusinessRuleException;
import com.brightloop.subscription.common.exception.DuplicateResourceException;
import com.brightloop.subscription.common.exception.ResourceNotFoundException;
import com.brightloop.subscription.feature.user.dto.CreateStaffRequest;
import com.brightloop.subscription.feature.user.dto.UpdateRoleRequest;
import com.brightloop.subscription.feature.user.dto.UpdateUserRequest;
import com.brightloop.subscription.feature.user.dto.UserResponse;
import com.brightloop.subscription.feature.user.entity.User;
import com.brightloop.subscription.feature.user.mapper.UserMapper;
import com.brightloop.subscription.feature.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserServiceImpl — user management operations for ADMIN.
 *
 * Business rules enforced here (not in the controller):
 * 1. Cannot create a CUSTOMER via this endpoint (use /api/auth/signup).
 * 2. An admin cannot change or deactivate their own account.
 *
 * @Transactional(readOnly=true) on read methods — signals the DB driver
 * that no DML will occur, enabling read optimisations.
 * @Transactional (default readOnly=false) on write methods — wraps the
 * update in a transaction; Hibernate dirty-checking auto-issues UPDATE SQL.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(Role role, String search, Pageable pageable) {
        Page<UserResponse> page = userRepository
                .findAllWithFilters(role, search, pageable)
                .map(userMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return userMapper.toResponse(findUserOrThrow(id));
    }

    @Override
    @Transactional
    public UserResponse createStaff(CreateStaffRequest request) {
        // Business rule: CUSTOMER accounts must be created via /api/auth/signup.
        if (request.role() == Role.CUSTOMER) {
            throw new BusinessRuleException(
                    "Cannot create a CUSTOMER user via this endpoint. " +
                    "Customers must register via /api/auth/signup.");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        // Default role to INTERNAL if not specified.
        Role assignedRole = (request.role() != null) ? request.role() : Role.INTERNAL;

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(assignedRole)
                .active(true)
                .build();

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request, Long requestingUserId) {
        // Business rule: admins cannot deactivate their own account
        if (id.equals(requestingUserId) && Boolean.FALSE.equals(request.active())) {
            throw new BusinessRuleException(
                    "Administrators cannot deactivate their own account. " +
                    "Please ask another admin to perform this action.");
        }

        User user = findUserOrThrow(id);
        user.setFullName(request.fullName());
        user.setActive(request.active());
        // No explicit save() needed — Hibernate dirty-checking flushes the change.
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateRole(Long id, UpdateRoleRequest request, Long requestingUserId) {
        // Business rule: admins cannot change their own role or deactivate themselves.
        if (id.equals(requestingUserId)) {
            throw new BusinessRuleException(
                    "Administrators cannot modify their own account. " +
                    "Please ask another admin to make this change.");
        }

        User user = findUserOrThrow(id);
        user.setRole(request.role());
        return userMapper.toResponse(user);
    }

    // ── Private helpers ──────────────────────────────────────────────

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
