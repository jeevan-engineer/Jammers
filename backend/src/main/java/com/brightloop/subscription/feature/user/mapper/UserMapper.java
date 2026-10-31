package com.brightloop.subscription.feature.user.mapper;

import com.brightloop.subscription.feature.user.dto.UserResponse;
import com.brightloop.subscription.feature.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * Manual mapper converting User entity to UserResponse DTO.
 *
 * @Component — registers this as a Spring bean so it can be injected
 * anywhere with @RequiredArgsConstructor / constructor injection.
 *
 * No MapStruct — per team standards, mappers are small @Component classes
 * written by hand. This keeps the dependency list small and is easy to debug.
 *
 * Never include passwordHash in the mapped output.
 */
@Component
public class UserMapper {

    /**
     * Maps a User entity to a UserResponse DTO.
     * Returns null if the input is null (safe for optional relationships).
     */
    public UserResponse toResponse(User user) {
        if (user == null) return null;
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
