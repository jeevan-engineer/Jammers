package com.brightloop.subscription.security;

import com.brightloop.subscription.feature.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads UserDetails from the database for Spring Security's authentication manager.
 *
 * @Transactional(readOnly=true) — opens a read-only Hibernate session so the
 * query is optimised by the DB. Read-only transactions cannot flush changes.
 *
 * This service is wired into the AuthenticationManager via SecurityConfig.
 * It is called during login (UsernamePasswordAuthenticationToken) and also
 * by JwtAuthFilter on every subsequent request.
 *
 * Common error: if this bean is missing or incorrectly named, Spring Security
 * auto-configures an InMemoryUserDetailsManager that ignores your DB entirely.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user by email (used as the "username").
     *
     * @throws UsernameNotFoundException if no user with this email exists.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with email: " + email));
    }
}
