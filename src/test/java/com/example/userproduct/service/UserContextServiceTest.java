package com.example.userproduct.service;

import com.example.userproduct.dao.UserRepository;
import com.example.userproduct.entities.User;
import com.example.userproduct.entities.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserContextService Tests")
class UserContextServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserContextService userContextService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id("user-1").name("John").email("john@test.com").role(UserRole.CUSTOMER).build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "john@test.com", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getCurrentUser returns user from security context")
    void getCurrentUser_returnsUser() {
        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));
        User result = userContextService.getCurrentUser();
        assertThat(result.getId()).isEqualTo("user-1");
    }

    @Test
    @DisplayName("getCurrentUser user not found throws UsernameNotFoundException")
    void getCurrentUser_notFound_throws() {
        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userContextService.getCurrentUser())
                .isInstanceOf(UsernameNotFoundException.class).hasMessage("Current user not found");
    }
}
