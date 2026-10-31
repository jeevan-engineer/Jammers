package com.brightloop.subscription.security;

import com.brightloop.subscription.common.dto.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles 401 Unauthorized responses for unauthenticated requests.
 *
 * AuthenticationEntryPoint — Spring Security's callback invoked when an
 * unauthenticated request reaches a protected resource. Without this,
 * Spring would redirect to /login or return an HTML error page.
 *
 * This implementation writes the ApiError JSON directly to the response,
 * consistent with our REST API error format.
 *
 * Wired into SecurityConfig via:
 *   httpSecurity.exceptionHandling(e -> e.authenticationEntryPoint(...))
 *
 * Common error: returning Content-Type: text/html causes JSON clients to
 * receive an unparseable response — always set APPLICATION_JSON.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiError body = ApiError.of(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Authentication required. Please provide a valid Bearer token.",
                request.getRequestURI()
        );

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
