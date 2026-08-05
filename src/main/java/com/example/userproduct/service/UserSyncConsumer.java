package com.example.userproduct.service;

import com.example.userproduct.dao.UserRepository;
import com.example.userproduct.entities.User;
import com.example.userproduct.events.EventPublisher;
import com.example.userproduct.events.UserEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Why: Responds to sync requests from order-delivery-service by publishing user data back
 *      on the user.events topic, ensuring downstream caches are populated with current data.
 * What: Listens to the "user-sync-request" topic and handles two request types:
 *       - FULL_SYNC: publishes all active users back on user.events with eventType USER_SYNC
 *       - USER_SYNC_REQUEST: looks up a specific user by ID and publishes their data back
 * Test: Mock UserRepository to return users, send a FULL_SYNC message, verify EventPublisher
 *       is called for each active user with eventType USER_SYNC.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSyncConsumer {

    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user-sync-request", groupId = "user-product-service-group")
    public void handleSyncRequest(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            String requestType = json.has("requestType") ? json.get("requestType").asText() : null;

            if (requestType == null) {
                log.warn("Received sync request with missing requestType, skipping");
                return;
            }

            switch (requestType) {
                case "FULL_SYNC":
                    handleFullSync();
                    break;
                case "USER_SYNC_REQUEST":
                    String userId = json.has("userId") ? json.get("userId").asText() : null;
                    if (userId != null && !userId.isBlank()) {
                        handleIndividualSync(userId);
                    } else {
                        log.warn("Received USER_SYNC_REQUEST with missing userId");
                    }
                    break;
                default:
                    log.warn("Unknown sync request type: {}", requestType);
            }
        } catch (Exception e) {
            log.error("Failed to process sync request: {}", e.getMessage(), e);
        }
    }

    /**
     * Why: Bulk-publishes all active users so downstream caches can be fully populated.
     * What: Fetches all active users and publishes each as a USER_SYNC event on user.events.
     * Test: Mock repository with 3 active users, call handleFullSync, verify 3 events published.
     */
    private void handleFullSync() {
        log.info("Processing FULL_SYNC request - publishing all active users");
        List<User> activeUsers = userRepository.findAll()
                .stream()
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .toList();

        int publishedCount = 0;
        for (User user : activeUsers) {
            try {
                publishUserSyncEvent(user);
                publishedCount++;
            } catch (Exception e) {
                log.warn("Failed to publish sync event for user {}: {}", user.getId(), e.getMessage());
            }
        }
        log.info("FULL_SYNC completed: published {} of {} active users", publishedCount, activeUsers.size());
    }

    /**
     * Why: Responds to a request for a specific user's data, typically when order-delivery-service
     *      detects an order referencing a user not in its cache.
     * What: Looks up the user by ID and publishes their data as a USER_SYNC event.
     * Test: Mock repository with user "u1", call handleIndividualSync("u1"), verify event published.
     */
    private void handleIndividualSync(String userId) {
        log.debug("Processing USER_SYNC_REQUEST for user: {}", userId);
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            publishUserSyncEvent(userOpt.get());
            log.info("Published USER_SYNC event for requested user: {}", userId);
        } else {
            log.warn("User {} not found for sync request", userId);
        }
    }

    /**
     * Why: Reuses the existing EventPublisher to publish user data in the standard envelope format.
     * What: Builds a UserEvent and publishes it with eventType USER_SYNC to user.events topic.
     * Test: Verify EventPublisher.publishUserEvent() is called with "USER_SYNC" and correct payload.
     */
    private void publishUserSyncEvent(User user) {
        UserEvent event = UserEvent.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .build();
        eventPublisher.publishUserEvent("USER_SYNC", event);
    }
}
