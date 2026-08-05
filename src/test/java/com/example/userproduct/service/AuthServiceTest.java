package com.example.userproduct.service;

import com.example.userproduct.dao.UserRepository;
import com.example.userproduct.dto.AuthResponse;
import com.example.userproduct.dto.LoginRequest;
import com.example.userproduct.dto.RegisterRequest;
import com.example.userproduct.entities.User;
import com.example.userproduct.entities.UserRole;
import com.example.userproduct.security.CustomUserDetailsService;
import com.example.userproduct.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User savedUser;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("password123")
                .phone("1234567890")
                .role(UserRole.CUSTOMER)
                .gender("Male")
                .age(30)
                .build();

        loginRequest = LoginRequest.builder()
                .email("john@example.com")
                .password("password123")
                .build();

        savedUser = User.builder()
                .id("user-uuid-1")
                .name("John Doe")
                .email("john@example.com")
                .password("encoded-password")
                .phone("1234567890")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();

        userDetails = new org.springframework.security.core.userdetails.User(
                "john@example.com",
                "encoded-password",
                true, true, true, true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
    }

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("Happy path - new user registers and receives enriched token")
        void register_happyPath_returnsAuthResponse() {
            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(userDetailsService.loadUserByUsername("john@example.com")).thenReturn(userDetails);
            when(jwtUtil.generateToken(any(Map.class), eq(userDetails))).thenReturn("jwt-token-123");

            AuthResponse response = authService.register(registerRequest);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt-token-123");
            assertThat(response.getUserId()).isEqualTo("user-uuid-1");
            assertThat(response.getEmail()).isEqualTo("john@example.com");
            assertThat(response.getName()).isEqualTo("John Doe");
            assertThat(response.getRole()).isEqualTo("CUSTOMER");
        }

        @Test
        @DisplayName("JWT is enriched with userId, role, and name claims")
        void register_jwtEnrichedWithClaims() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(jwtUtil.generateToken(any(Map.class), any(UserDetails.class))).thenReturn("token");

            authService.register(registerRequest);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> claimsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(jwtUtil).generateToken(claimsCaptor.capture(), eq(userDetails));

            Map<String, Object> claims = claimsCaptor.getValue();
            assertThat(claims).containsEntry("userId", "user-uuid-1");
            assertThat(claims).containsEntry("role", "CUSTOMER");
            assertThat(claims).containsEntry("name", "John Doe");
        }

        @Test
        @DisplayName("Duplicate email throws IllegalArgumentException")
        void register_duplicateEmail_throwsIllegalArgumentException() {
            when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email already registered");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Password is BCrypt encoded before saving")
        void register_passwordIsEncoded() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
            when(jwtUtil.generateToken(any(Map.class), any(UserDetails.class))).thenReturn("token");

            authService.register(registerRequest);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-password");
            verify(passwordEncoder).encode("password123");
        }
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("Valid credentials return enriched token")
        void login_validCredentials_returnsAuthResponse() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(savedUser));
            when(userDetailsService.loadUserByUsername("john@example.com")).thenReturn(userDetails);
            when(jwtUtil.generateToken(any(Map.class), eq(userDetails))).thenReturn("jwt-token-123");

            AuthResponse response = authService.login(loginRequest);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt-token-123");
            assertThat(response.getEmail()).isEqualTo("john@example.com");
            assertThat(response.getRole()).isEqualTo("CUSTOMER");
        }

        @Test
        @DisplayName("Bad credentials propagate exception")
        void login_badCredentials_throwsException() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }
}
