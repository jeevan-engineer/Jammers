package com.brightloop.subscription.feature.auth.service;

import com.brightloop.subscription.common.enums.Role;
import com.brightloop.subscription.common.exception.DuplicateResourceException;
import com.brightloop.subscription.common.exception.ResourceNotFoundException;
import com.brightloop.subscription.feature.auth.dto.AuthResponse;
import com.brightloop.subscription.feature.auth.dto.LoginRequest;
import com.brightloop.subscription.feature.auth.dto.SignupRequest;
import com.brightloop.subscription.feature.customer.entity.Customer;
import com.brightloop.subscription.feature.customer.repository.CustomerRepository;
import com.brightloop.subscription.feature.user.dto.UserResponse;
import com.brightloop.subscription.feature.user.entity.User;
import com.brightloop.subscription.feature.user.mapper.UserMapper;
import com.brightloop.subscription.feature.user.repository.UserRepository;
import com.brightloop.subscription.security.CustomUserDetails;
import com.brightloop.subscription.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthServiceImpl — concrete implementation of AuthService.
 *
 * @Transactional on signup() — ensures the User INSERT and Customer INSERT
 * are both committed or both rolled back. If Customer creation fails after
 * User is persisted, the whole transaction is rolled back (no orphan user row).
 *
 * AuthenticationManager.authenticate() — Spring Security handles:
 *   1. Loading user via CustomUserDetailsService.loadUserByUsername(email).
 *   2. Comparing password via BCryptPasswordEncoder.matches().
 *   3. Checking isEnabled() — throws DisabledException if false.
 *   4. Throwing BadCredentialsException if password doesn't match.
 * We do NOT manually call passwordEncoder.matches() here; the AuthManager does it.
 *
 * Common error: injecting PasswordEncoder and AuthManager in the same class
 * that defines the AuthenticationManager bean can cause circular dependency.
 * We resolved this by defining those beans in SecurityConfig (a separate class).
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    /**
     * Signup flow:
     * 1. Check email uniqueness.
     * 2. Create User with role=CUSTOMER and hashed password.
     * 3. Create linked Customer profile with name and email from signup form.
     * 4. Persist both in a single transaction.
     * 5. Return a signed JWT in AuthResponse.
     */
    @Override
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email()) || customerRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        // Build and save the User first (so Customer can reference it).
        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .active(true)
                .build();
        user = userRepository.save(user);

        // Build and save the linked Customer profile.
        Customer customer = Customer.builder()
                .user(user)
                .name(request.fullName())
                .email(request.email())
                .active(true)
                .build();
        customerRepository.save(customer);

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getId()
        );

        return AuthResponse.bearer(
                token,
                jwtService.getExpirationMs(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    /**
     * Login flow:
     * 1. Delegate to AuthenticationManager (validates password + active status).
     * 2. Extract CustomUserDetails from the authenticated principal.
     * 3. Generate JWT and return AuthResponse.
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        // authenticate() will throw BadCredentialsException or DisabledException on failure.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getId()
        );

        return AuthResponse.bearer(
                token,
                jwtService.getExpirationMs(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    /** Returns the user profile for the currently authenticated principal. */
    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return userMapper.toResponse(user);
    }
}
