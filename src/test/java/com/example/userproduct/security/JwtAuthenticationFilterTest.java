package com.example.userproduct.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;
    @InjectMocks private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach void setUp() {
        SecurityContextHolder.clearContext();
    }
    @AfterEach void tearDown() { SecurityContextHolder.clearContext(); }

    @Test @DisplayName("No Authorization header - no auth set")
    void doFilterInternal_noAuthHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test @DisplayName("Non-Bearer header - no auth set")
    void doFilterInternal_nonBearerHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test @DisplayName("Valid Bearer token - SecurityContext populated")
    void doFilterInternal_validBearerToken() throws ServletException, IOException {
        String token = "valid-jwt-token";
        UserDetails userDetails = new User("john@example.com", "password", true, true, true, true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractUsername(token)).thenReturn("john@example.com");
        when(this.userDetailsService.loadUserByUsername("john@example.com")).thenReturn(userDetails);
        when(jwtUtil.validateToken(token, userDetails)).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("john@example.com");
    }

    @Test @DisplayName("Invalid token - no auth set")
    void doFilterInternal_invalidToken() throws ServletException, IOException {
        String token = "invalid-jwt-token";
        UserDetails userDetails = new User("john@example.com", "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractUsername(token)).thenReturn("john@example.com");
        when(this.userDetailsService.loadUserByUsername("john@example.com")).thenReturn(userDetails);
        when(jwtUtil.validateToken(token, userDetails)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
