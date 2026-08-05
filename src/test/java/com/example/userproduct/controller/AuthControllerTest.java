package com.example.userproduct.controller;

import com.example.userproduct.dto.AuthResponse;
import com.example.userproduct.dto.LoginRequest;
import com.example.userproduct.dto.RegisterRequest;
import com.example.userproduct.entities.UserRole;
import com.example.userproduct.security.CustomUserDetailsService;
import com.example.userproduct.security.JwtUtil;
import com.example.userproduct.security.SecurityConfig;
import com.example.userproduct.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private AuthService authService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Nested @DisplayName("POST /api/auth/register") class RegisterTests {
        @Test @DisplayName("Valid registration returns 201")
        void register_validRequest_returns201() throws Exception {
            RegisterRequest request = RegisterRequest.builder().name("John Doe").email("john@example.com")
                    .password("password123").phone("1234567890").role(UserRole.CUSTOMER).build();
            AuthResponse response = AuthResponse.builder().token("jwt-token-123").userId("user-1")
                    .email("john@example.com").name("John Doe").role("CUSTOMER").build();
            when(authService.register(any(RegisterRequest.class))).thenReturn(response);
            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").value("jwt-token-123"))
                    .andExpect(jsonPath("$.userId").value("user-1"));
        }

        @Test @DisplayName("Missing required fields returns 400")
        void register_missingFields_returns400() throws Exception {
            RegisterRequest request = RegisterRequest.builder().name("").email("").password("").phone("").role(null).build();
            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("Invalid email format returns 400")
        void register_invalidEmail_returns400() throws Exception {
            RegisterRequest request = RegisterRequest.builder().name("John Doe").email("not-an-email")
                    .password("password123").phone("1234567890").role(UserRole.CUSTOMER).build();
            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("Short password returns 400")
        void register_shortPassword_returns400() throws Exception {
            RegisterRequest request = RegisterRequest.builder().name("John Doe").email("john@example.com")
                    .password("abc").phone("1234567890").role(UserRole.CUSTOMER).build();
            mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested @DisplayName("POST /api/auth/login") class LoginTests {
        @Test @DisplayName("Valid login returns 200")
        void login_validCredentials_returns200() throws Exception {
            LoginRequest request = LoginRequest.builder().email("john@example.com").password("password123").build();
            AuthResponse response = AuthResponse.builder().token("jwt-token-456").userId("user-1")
                    .email("john@example.com").name("John Doe").role("CUSTOMER").build();
            when(authService.login(any(LoginRequest.class))).thenReturn(response);
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.token").value("jwt-token-456"));
        }

        @Test @DisplayName("Bad credentials returns 401")
        void login_badCredentials_returns401() throws Exception {
            LoginRequest request = LoginRequest.builder().email("john@example.com").password("wrongpassword").build();
            when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("Invalid credentials"));
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("Missing email returns 400")
        void login_missingEmail_returns400() throws Exception {
            LoginRequest request = LoginRequest.builder().email("").password("password123").build();
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested @DisplayName("POST /api/auth/logout") class LogoutTests {
        @Test @DisplayName("Authenticated logout returns 200")
        void logout_authenticated_returns200() throws Exception {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    "user@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
            mockMvc.perform(post("/api/auth/logout").with(authentication(auth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logout successful. Please discard your token."));
        }
    }
}
