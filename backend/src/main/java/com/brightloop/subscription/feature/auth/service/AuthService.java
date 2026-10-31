package com.brightloop.subscription.feature.auth.service;

import com.brightloop.subscription.feature.auth.dto.AuthResponse;
import com.brightloop.subscription.feature.auth.dto.LoginRequest;
import com.brightloop.subscription.feature.auth.dto.SignupRequest;
import com.brightloop.subscription.feature.user.dto.UserResponse;

/**
 * Auth service interface — defines the contract for the auth feature.
 * Having an interface + Impl class follows the team's standard and
 * allows easy mocking in tests.
 */
public interface AuthService {

    /**
     * Registers a new CUSTOMER user and creates their customer profile atomically.
     * @throws com.brightloop.subscription.common.exception.DuplicateResourceException if email is taken.
     */
    AuthResponse signup(SignupRequest request);

    /**
     * Authenticates a user by email/password and returns a JWT.
     * @throws org.springframework.security.authentication.BadCredentialsException for wrong credentials.
     * @throws org.springframework.security.authentication.DisabledException for inactive accounts.
     */
    AuthResponse login(LoginRequest request);

    /**
     * Returns the profile of the currently authenticated user.
     * @param email the email extracted from the JWT by the controller.
     */
    UserResponse getCurrentUser(String email);
}
