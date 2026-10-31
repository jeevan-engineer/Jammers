package com.brightloop.subscription.feature.auth;

import com.brightloop.subscription.common.enums.Role;
import com.brightloop.subscription.common.exception.DuplicateResourceException;
import com.brightloop.subscription.feature.auth.dto.AuthResponse;
import com.brightloop.subscription.feature.auth.dto.SignupRequest;
import com.brightloop.subscription.feature.auth.service.AuthServiceImpl;
import com.brightloop.subscription.feature.customer.entity.Customer;
import com.brightloop.subscription.feature.customer.repository.CustomerRepository;
import com.brightloop.subscription.feature.user.entity.User;
import com.brightloop.subscription.feature.user.mapper.UserMapper;
import com.brightloop.subscription.feature.user.repository.UserRepository;
import com.brightloop.subscription.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("signup() throws DuplicateResourceException when email is already registered")
    void signup_duplicateEmail_throwsDuplicateResourceException() {
        // Given
        SignupRequest request = new SignupRequest("Jane Doe", "jane@example.com", "Password@123");
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("jane@example.com");

        verify(userRepository, times(1)).existsByEmail("jane@example.com");
        verify(userRepository, never()).save(any(User.class));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("signup() successfully creates user, customer profile, and returns AuthResponse with CUSTOMER role")
    void signup_success_returnsAuthResponse() {
        // Given
        SignupRequest request = new SignupRequest("Jane Doe", "jane@example.com", "Password@123");
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("encodedPasswordHash");

        User savedUser = User.builder()
                .id(42L)
                .fullName("Jane Doe")
                .email("jane@example.com")
                .passwordHash("encodedPasswordHash")
                .role(Role.CUSTOMER)
                .active(true)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        when(jwtService.generateToken("jane@example.com", "CUSTOMER", 42L)).thenReturn("mocked.jwt.token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        // When
        AuthResponse response = authService.signup(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("mocked.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.role()).isEqualTo("CUSTOMER");
        assertThat(response.email()).isEqualTo("jane@example.com");
        assertThat(response.userId()).isEqualTo(42L);

        verify(userRepository, times(1)).save(any(User.class));
        verify(customerRepository, times(1)).save(any(Customer.class));
        verify(jwtService, times(1)).generateToken("jane@example.com", "CUSTOMER", 42L);
    }
}
