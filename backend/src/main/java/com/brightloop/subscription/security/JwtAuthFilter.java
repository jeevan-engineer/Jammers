package com.brightloop.subscription.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter — runs once per HTTP request.
 *
 * OncePerRequestFilter — Spring's base class that guarantees this filter is
 * invoked exactly once per request, even in forward/include chains.
 *
 * Flow:
 *  1. Extract "Bearer <token>" from the Authorization header.
 *  2. Validate the JWT with JwtService.
 *  3. Load UserDetails from DB via CustomUserDetailsService.
 *  4. Set a UsernamePasswordAuthenticationToken in the SecurityContext.
 *
 * If the token is absent/invalid we do NOT throw — we simply let the
 * request proceed unauthenticated. The SecurityConfig will then reject
 * requests to protected endpoints with a 401 via RestAuthenticationEntryPoint.
 *
 * Common error: if the filter sets authentication even for invalid tokens,
 * users could access protected resources without valid credentials.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // No Authorization header or not a Bearer token — skip JWT processing.
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(BEARER_PREFIX.length());

        // Validate token before touching the SecurityContext.
        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String email = jwtService.extractEmail(token);

        // Only authenticate if not already authenticated in this request.
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // isEnabled() check — reject inactive users even with a valid token.
            if (userDetails.isEnabled()) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
