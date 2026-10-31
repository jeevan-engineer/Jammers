package com.brightloop.subscription.feature.customer;

import com.brightloop.subscription.common.enums.Role;
import com.brightloop.subscription.common.exception.ResourceNotFoundException;
import com.brightloop.subscription.feature.customer.dto.CustomerResponse;
import com.brightloop.subscription.feature.customer.entity.Customer;
import com.brightloop.subscription.feature.customer.mapper.CustomerMapper;
import com.brightloop.subscription.feature.customer.repository.CustomerRepository;
import com.brightloop.subscription.feature.customer.service.CustomerServiceImpl;
import com.brightloop.subscription.feature.user.entity.User;
import com.brightloop.subscription.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerServiceImpl customerService;

    @Test
    @DisplayName("getCustomerById() throws ResourceNotFoundException when customer does not exist")
    void getCustomerById_notFound_throwsResourceNotFoundException() {
        // Given
        Long nonExistentId = 999L;
        User adminUser = User.builder().id(1L).email("admin@test.com").role(Role.ADMIN).active(true).build();
        CustomUserDetails adminPrincipal = new CustomUserDetails(adminUser);

        when(customerRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> customerService.getCustomerById(nonExistentId, adminPrincipal))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer");

        verify(customerRepository, times(1)).findById(nonExistentId);
        verify(customerMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("getCustomerById() throws AccessDeniedException when CUSTOMER role accesses another user's profile")
    void getCustomerById_unauthorizedCustomer_throwsAccessDeniedException() {
        // Given
        Long targetCustomerId = 10L;

        // Customer owner is user 200
        User ownerUser = User.builder().id(200L).email("owner@test.com").role(Role.CUSTOMER).active(true).build();
        Customer targetCustomer = Customer.builder()
                .id(targetCustomerId)
                .name("Owner")
                .email("owner@test.com")
                .user(ownerUser)
                .active(true)
                .build();

        // Caller is user 100 (different customer)
        User callerUser = User.builder().id(100L).email("intruder@test.com").role(Role.CUSTOMER).active(true).build();
        CustomUserDetails callerPrincipal = new CustomUserDetails(callerUser);

        when(customerRepository.findById(targetCustomerId)).thenReturn(Optional.of(targetCustomer));

        // When & Then
        assertThatThrownBy(() -> customerService.getCustomerById(targetCustomerId, callerPrincipal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You can only access your own customer profile");

        verify(customerRepository, times(1)).findById(targetCustomerId);
        verify(customerMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("getCustomerById() returns CustomerResponse when CUSTOMER accesses their own profile")
    void getCustomerById_ownerCustomer_returnsResponse() {
        // Given
        Long customerId = 10L;
        Long userId = 100L;

        User user = User.builder().id(userId).email("user@test.com").role(Role.CUSTOMER).active(true).build();
        Customer customer = Customer.builder()
                .id(customerId)
                .name("Acme Corp")
                .email("user@test.com")
                .user(user)
                .active(true)
                .build();
        CustomUserDetails principal = new CustomUserDetails(user);

        CustomerResponse expectedResponse = new CustomerResponse(
                customerId, userId, "Acme Corp", "user@test.com", null, null, null, null, true,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerMapper.toResponse(customer)).thenReturn(expectedResponse);

        // When
        CustomerResponse actual = customerService.getCustomerById(customerId, principal);

        // Then
        assertThat(actual).isNotNull();
        assertThat(actual.id()).isEqualTo(customerId);
        assertThat(actual.email()).isEqualTo("user@test.com");
        verify(customerMapper, times(1)).toResponse(customer);
    }
}
