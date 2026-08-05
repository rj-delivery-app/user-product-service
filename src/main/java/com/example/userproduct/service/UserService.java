package com.example.userproduct.service;

import com.example.userproduct.aop.PublishKafkaEvent;
import com.example.userproduct.dao.UserRepository;
import com.example.userproduct.dto.UpdateUserProfileRequest;
import com.example.userproduct.dto.UserProfileResponse;
import com.example.userproduct.entities.User;
import com.example.userproduct.entities.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for user management operations.
 *
 * Why: Encapsulates all user CRUD logic; Kafka events are published declaratively
 *      via @PublishKafkaEvent annotations handled by KafkaEventAspect.
 * What: Manages user profiles, activation/deactivation, and listing with pagination.
 * Test: Mock UserRepository; verify profile mapping and correct return values.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Get user profile by user ID
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return mapToProfileResponse(user);
    }

    /**
     * Update user profile (name, phone, gender, age)
     */
    @Transactional
    @PublishKafkaEvent(eventType = "USER_UPDATED", topic = "user")
    public UserProfileResponse updateUserProfile(String userId, UpdateUserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setGender(request.getGender());
        user.setAge(request.getAge());

        user = userRepository.save(user);
        log.info("User profile updated for user: {}", userId);

        return mapToProfileResponse(user);
    }

    /**
     * Get user by ID (admin view)
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return mapToProfileResponse(user);
    }

    /**
     * List all users with pagination
     */
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> getAllUsers(int pageNo, int size) {
        PageRequest pageable = PageRequest.of(pageNo, size);
        return userRepository.findAll(pageable)
                .map(this::mapToProfileResponse);
    }

    /**
     * Deactivate user (soft-delete)
     */
    @Transactional
    public UserProfileResponse deactivateUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setIsActive(false);
        user = userRepository.save(user);
        log.info("User deactivated: {}", userId);

        return mapToProfileResponse(user);
    }

    /**
     * Activate user
     */
    @Transactional
    public UserProfileResponse activateUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setIsActive(true);
        user = userRepository.save(user);
        log.info("User activated: {}", userId);

        return mapToProfileResponse(user);
    }

    /**
     * Why: Enables merchants to see available riders when assigning orders.
     * What: Returns all active users with DELIVERY_RIDER role.
     * Test: Mock repository to return 2 active riders, call getRidersList, assert list has 2 entries.
     */
    @Transactional(readOnly = true)
    public List<UserProfileResponse> getRidersList() {
        return userRepository.findByRoleAndIsActiveTrue(UserRole.DELIVERY_RIDER)
                .stream()
                .map(this::mapToProfileResponse)
                .toList();
    }

    /**
     * Map User entity to UserProfileResponse DTO
     */
    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .gender(user.getGender())
                .age(user.getAge())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
