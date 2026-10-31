package com.brightloop.subscription.feature.user.service;

import com.brightloop.subscription.common.dto.PageResponse;
import com.brightloop.subscription.common.enums.Role;
import com.brightloop.subscription.feature.user.dto.CreateStaffRequest;
import com.brightloop.subscription.feature.user.dto.UpdateRoleRequest;
import com.brightloop.subscription.feature.user.dto.UpdateUserRequest;
import com.brightloop.subscription.feature.user.dto.UserResponse;
import org.springframework.data.domain.Pageable;

public interface UserService {

    PageResponse<UserResponse> getAllUsers(Role role, String search, Pageable pageable);

    UserResponse getUserById(Long id);

    UserResponse createStaff(CreateStaffRequest request);

    UserResponse updateUser(Long id, UpdateUserRequest request);

    /**
     * Updates the role of a user.
     * @param requestingUserId the ID of the admin making the request (for self-protection check)
     */
    UserResponse updateRole(Long id, UpdateRoleRequest request, Long requestingUserId);
}
