package com.example.userproduct.service;

import com.example.userproduct.dao.UserRepository;
import com.example.userproduct.dto.UpdateUserProfileRequest;
import com.example.userproduct.dto.UserProfileResponse;
import com.example.userproduct.entities.User;
import com.example.userproduct.entities.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;
    private final String userId = "user-uuid-1";

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(userId)
                .name("John Doe")
                .email("john@example.com")
                .password("encoded")
                .phone("1234567890")
                .role(UserRole.CUSTOMER)
                .gender("Male")
                .age(30)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("getUserProfile()")
    class GetUserProfile {

        @Test
        @DisplayName("Found user returns mapped response")
        void getUserProfile_found_returnsResponse() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            UserProfileResponse response = userService.getUserProfile(userId);

            assertThat(response.getId()).isEqualTo(userId);
            assertThat(response.getName()).isEqualTo("John Doe");
            assertThat(response.getEmail()).isEqualTo("john@example.com");
            assertThat(response.getPhone()).isEqualTo("1234567890");
            assertThat(response.getRole()).isEqualTo("CUSTOMER");
            assertThat(response.getGender()).isEqualTo("Male");
            assertThat(response.getAge()).isEqualTo(30);
            assertThat(response.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("User not found throws IllegalArgumentException")
        void getUserProfile_notFound_throws() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserProfile(userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User not found");
        }
    }

    @Nested
    @DisplayName("updateUserProfile()")
    class UpdateUserProfile {

        @Test
        @DisplayName("Updates fields correctly")
        void updateUserProfile_updatesFields() {
            UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                    .name("Jane Doe")
                    .phone("0987654321")
                    .gender("Female")
                    .age(25)
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserProfileResponse response = userService.updateUserProfile(userId, request);

            assertThat(response.getName()).isEqualTo("Jane Doe");
            assertThat(response.getPhone()).isEqualTo("0987654321");
            assertThat(response.getGender()).isEqualTo("Female");
            assertThat(response.getAge()).isEqualTo(25);
        }

        @Test
        @DisplayName("User not found throws")
        void updateUserProfile_notFound_throws() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            UpdateUserProfileRequest request = UpdateUserProfileRequest.builder()
                    .name("Jane").phone("123").build();

            assertThatThrownBy(() -> userService.updateUserProfile(userId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User not found");
        }
    }

    @Nested
    @DisplayName("getUserById()")
    class GetUserById {

        @Test
        @DisplayName("Found returns response")
        void getUserById_found_returnsResponse() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            UserProfileResponse response = userService.getUserById(userId);

            assertThat(response.getId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Not found throws")
        void getUserById_notFound_throws() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User not found");
        }
    }

    @Nested
    @DisplayName("getRidersList()")
    class GetRidersList {

        @Test
        @DisplayName("Returns active delivery riders")
        void getRidersList_returnsActiveRiders() {
            User rider1 = User.builder()
                    .id("rider-1").name("Rider One").email("rider1@example.com")
                    .password("encoded").phone("111").role(UserRole.DELIVERY_RIDER)
                    .gender("Male").age(25).isActive(true)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();
            User rider2 = User.builder()
                    .id("rider-2").name("Rider Two").email("rider2@example.com")
                    .password("encoded").phone("222").role(UserRole.DELIVERY_RIDER)
                    .gender("Female").age(28).isActive(true)
                    .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                    .build();

            when(userRepository.findByRoleAndIsActiveTrue(UserRole.DELIVERY_RIDER))
                    .thenReturn(List.of(rider1, rider2));

            List<UserProfileResponse> result = userService.getRidersList();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Rider One");
            assertThat(result.get(1).getName()).isEqualTo("Rider Two");
            assertThat(result.get(0).getRole()).isEqualTo("DELIVERY_RIDER");
        }

        @Test
        @DisplayName("Returns empty list when no active riders")
        void getRidersList_noRiders_returnsEmpty() {
            when(userRepository.findByRoleAndIsActiveTrue(UserRole.DELIVERY_RIDER))
                    .thenReturn(List.of());

            List<UserProfileResponse> result = userService.getRidersList();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAllUsers()")
    class GetAllUsers {

        @Test
        @DisplayName("Returns paginated results")
        void getAllUsers_returnsPaginatedResults() {
            PageRequest pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);
            when(userRepository.findAll(pageable)).thenReturn(userPage);

            Page<UserProfileResponse> result = userService.getAllUsers(0, 10);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(userId);
        }
    }

    @Nested
    @DisplayName("deactivateUser()")
    class DeactivateUser {

        @Test
        @DisplayName("Sets isActive to false")
        void deactivateUser_setsIsActiveFalse() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserProfileResponse response = userService.deactivateUser(userId);

            assertThat(response.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("Not found throws")
        void deactivateUser_notFound_throws() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deactivateUser(userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User not found");
        }
    }

    @Nested
    @DisplayName("activateUser()")
    class ActivateUser {

        @Test
        @DisplayName("Sets isActive to true")
        void activateUser_setsIsActiveTrue() {
            user.setIsActive(false);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserProfileResponse response = userService.activateUser(userId);

            assertThat(response.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("Not found throws")
        void activateUser_notFound_throws() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.activateUser(userId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User not found");
        }
    }
}
