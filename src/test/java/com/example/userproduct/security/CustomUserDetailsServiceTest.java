package com.example.userproduct.security;

import com.example.userproduct.dao.UserRepository;
import com.example.userproduct.entities.User;
import com.example.userproduct.entities.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Tests")
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private CustomUserDetailsService customUserDetailsService;
    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id("user-1").name("John Doe").email("john@example.com")
                .password("encoded-password").phone("1234567890").role(UserRole.CUSTOMER).isActive(true).build();
    }

    @Test @DisplayName("loadUserByUsername found returns correct UserDetails")
    void loadUserByUsername_found_returnsCorrectUserDetails() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("john@example.com");
        assertThat(userDetails.getUsername()).isEqualTo("john@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_CUSTOMER");
    }

    @Test @DisplayName("loadUserByUsername MERCHANT_ADMIN has correct role")
    void loadUserByUsername_merchantAdmin_hasCorrectRole() {
        user.setRole(UserRole.MERCHANT_ADMIN);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("john@example.com");
        assertThat(userDetails.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_MERCHANT_ADMIN");
    }

    @Test @DisplayName("loadUserByUsername not found throws")
    void loadUserByUsername_notFound_throws() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class).hasMessageContaining("User not found with email: unknown@example.com");
    }

    @Test @DisplayName("Inactive user has isEnabled=false")
    void loadUserByUsername_inactiveUser_isNotEnabled() {
        user.setIsActive(false);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("john@example.com");
        assertThat(userDetails.isEnabled()).isFalse();
    }
}
