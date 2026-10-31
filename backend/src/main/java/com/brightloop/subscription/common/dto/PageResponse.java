package com.brightloop.subscription.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic paginated response wrapper for all list APIs.
 *
 * Spring's Page<T> contains the data but serialises to a complex nested JSON.
 * This DTO flattens the response to a clean, frontend-friendly structure.
 *
 * Usage:
 *   Page<UserResponse> page = userRepository.findAll(pageable).map(userMapper::toResponse);
 *   return PageResponse.from(page);
 *
 * @param <T> the DTO type (never entities — entities must be mapped first).
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    /**
     * Static factory that creates a PageResponse from a Spring Data Page.
     *
     * @param springPage a mapped Page<T> (already containing DTOs, not entities)
     */
    public static <T> PageResponse<T> from(Page<T> springPage) {
        return new PageResponse<>(
                springPage.getContent(),
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.isLast()
        );
    }
}
