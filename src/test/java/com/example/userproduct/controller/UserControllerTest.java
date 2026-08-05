package com.example.userproduct.controller;

import com.example.userproduct.dto.UpdateUserProfileRequest;
import com.example.userproduct.dto.UserProfileResponse;
import com.example.userproduct.security.CustomUserDetailsService;
import com.example.userproduct.security.JwtUtil;
import com.example.userproduct.security.SecurityConfig;
import com.example.userproduct.service.UserContextService;
import com.example.userproduct.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class) @AutoConfigureMockMvc @Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private UserService userService;
    @MockBean private UserContextService userContextService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    private UsernamePasswordAuthenticationToken authAs(String role) {
        return new UsernamePasswordAuthenticationToken("user@example.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    private UserProfileResponse buildSampleProfile() {
        return UserProfileResponse.builder().id("user-1").name("John Doe").email("john@example.com")
                .phone("1234567890").role("CUSTOMER").gender("Male").age(30).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    @Nested @DisplayName("GET /api/users/profile") class GetProfileTests {
        @Test @DisplayName("Authenticated user gets profile - 200")
        void getProfile_authenticated_returns200() throws Exception {
            when(userContextService.getCurrentUserId()).thenReturn("user-1");
            when(userService.getUserProfile("user-1")).thenReturn(buildSampleProfile());
            mockMvc.perform(get("/api/users/profile").with(authentication(authAs("CUSTOMER"))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("user-1"));
        }
        @Test @DisplayName("No authentication returns 401")
        void getProfile_noAuth_returns401() throws Exception {
            mockMvc.perform(get("/api/users/profile")).andExpect(status().isUnauthorized());
        }
    }

    @Nested @DisplayName("PUT /api/users/profile") class UpdateProfileTests {
        @Test @DisplayName("Valid update returns 200")
        void updateProfile_valid_returns200() throws Exception {
            UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                    .name("Jane Doe").phone("9876543210").gender("Female").age(25).build();
            UserProfileResponse updated = buildSampleProfile();
            updated.setName("Jane Doe");
            when(userContextService.getCurrentUserId()).thenReturn("user-1");
            when(userService.updateUserProfile(eq("user-1"), any(UpdateUserProfileRequest.class))).thenReturn(updated);
            mockMvc.perform(put("/api/users/profile").with(authentication(authAs("CUSTOMER")))
                            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Jane Doe"));
        }
        @Test @DisplayName("Invalid data returns 400")
        void updateProfile_invalidData_returns400() throws Exception {
            UpdateUserProfileRequest request = UpdateUserProfileRequest.builder().name("").phone("").build();
            mockMvc.perform(put("/api/users/profile").with(authentication(authAs("CUSTOMER")))
                            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested @DisplayName("GET /api/users/riders") class GetAvailableRidersTests {
        @Test @DisplayName("MERCHANT_ADMIN gets riders list - 200")
        void getRiders_merchantAdmin_returns200() throws Exception {
            UserProfileResponse rider = buildSampleProfile();
            rider.setRole("DELIVERY_RIDER");
            rider.setName("Rider One");
            when(userService.getRidersList()).thenReturn(List.of(rider));
            mockMvc.perform(get("/api/users/riders").with(authentication(authAs("MERCHANT_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].role").value("DELIVERY_RIDER"));
        }
        @Test @DisplayName("CUSTOMER role gets 403")
        void getRiders_customer_returns403() throws Exception {
            mockMvc.perform(get("/api/users/riders").with(authentication(authAs("CUSTOMER"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested @DisplayName("GET /api/users/{id}") class GetUserByIdTests {
        @Test @DisplayName("MERCHANT_ADMIN can get user by ID - 200")
        void getUserById_merchantAdmin_returns200() throws Exception {
            when(userService.getUserById("user-1")).thenReturn(buildSampleProfile());
            mockMvc.perform(get("/api/users/{id}", "user-1").with(authentication(authAs("MERCHANT_ADMIN"))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("user-1"));
        }
        @Test @DisplayName("CUSTOMER role gets 403")
        void getUserById_customer_returns403() throws Exception {
            mockMvc.perform(get("/api/users/{id}", "user-1").with(authentication(authAs("CUSTOMER"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested @DisplayName("GET /api/users") class GetAllUsersTests {
        @Test @DisplayName("MERCHANT_ADMIN gets paginated users - 200")
        void getAllUsers_merchantAdmin_returns200() throws Exception {
            Page<UserProfileResponse> page = new PageImpl<>(List.of(buildSampleProfile()));
            when(userService.getAllUsers(any(Integer.class), any(Integer.class))).thenReturn(page);
            mockMvc.perform(get("/api/users").with(authentication(authAs("MERCHANT_ADMIN"))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value("user-1"));
        }
        @Test @DisplayName("CUSTOMER role gets 403")
        void getAllUsers_customer_returns403() throws Exception {
            mockMvc.perform(get("/api/users").with(authentication(authAs("CUSTOMER"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested @DisplayName("PATCH /api/users/{id}/deactivate") class DeactivateUserTests {
        @Test @DisplayName("MERCHANT_ADMIN can deactivate user - 200")
        void deactivateUser_merchantAdmin_returns200() throws Exception {
            UserProfileResponse profile = buildSampleProfile();
            profile.setIsActive(false);
            when(userService.deactivateUser("user-1")).thenReturn(profile);
            mockMvc.perform(patch("/api/users/{id}/deactivate", "user-1").with(authentication(authAs("MERCHANT_ADMIN"))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.isActive").value(false));
        }
        @Test @DisplayName("CUSTOMER role gets 403")
        void deactivateUser_customer_returns403() throws Exception {
            mockMvc.perform(patch("/api/users/{id}/deactivate", "user-1").with(authentication(authAs("CUSTOMER"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested @DisplayName("PATCH /api/users/{id}/activate") class ActivateUserTests {
        @Test @DisplayName("MERCHANT_ADMIN can activate user - 200")
        void activateUser_merchantAdmin_returns200() throws Exception {
            when(userService.activateUser("user-1")).thenReturn(buildSampleProfile());
            mockMvc.perform(patch("/api/users/{id}/activate", "user-1").with(authentication(authAs("MERCHANT_ADMIN"))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.isActive").value(true));
        }
        @Test @DisplayName("CUSTOMER role gets 403")
        void activateUser_customer_returns403() throws Exception {
            mockMvc.perform(patch("/api/users/{id}/activate", "user-1").with(authentication(authAs("CUSTOMER"))))
                    .andExpect(status().isForbidden());
        }
    }
}
