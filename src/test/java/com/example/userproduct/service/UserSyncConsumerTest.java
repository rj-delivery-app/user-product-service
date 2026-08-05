package com.example.userproduct.service;

import com.example.userproduct.dao.UserRepository;
import com.example.userproduct.entities.User;
import com.example.userproduct.entities.UserRole;
import com.example.userproduct.events.EventPublisher;
import com.example.userproduct.events.UserEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSyncConsumer Tests")
class UserSyncConsumerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private UserSyncConsumer userSyncConsumer;

    @Test
    @DisplayName("FULL_SYNC publishes all active users")
    void handleSyncRequest_fullSync_publishesAllActiveUsers() {
        String message = """
                {"requestType":"FULL_SYNC","timestamp":"2025-01-01T00:00:00Z","requestedBy":"order-delivery-service"}
                """;

        User user1 = User.builder().id("u1").name("Alice").email("alice@test.com")
                .phone("123").role(UserRole.CUSTOMER).isActive(true).build();
        User user2 = User.builder().id("u2").name("Bob").email("bob@test.com")
                .phone("456").role(UserRole.MERCHANT_ADMIN).isActive(true).build();
        User inactiveUser = User.builder().id("u3").name("Charlie").email("charlie@test.com")
                .phone("789").role(UserRole.DELIVERY_RIDER).isActive(false).build();

        when(userRepository.findAll()).thenReturn(List.of(user1, user2, inactiveUser));

        userSyncConsumer.handleSyncRequest(message);

        verify(eventPublisher, times(2)).publishUserEvent(eq("USER_SYNC"), any(UserEvent.class));
    }

    @Test
    @DisplayName("USER_SYNC_REQUEST publishes specific user")
    void handleSyncRequest_individualSync_publishesSpecificUser() {
        String message = """
                {"requestType":"USER_SYNC_REQUEST","userId":"u1","timestamp":"2025-01-01T00:00:00Z"}
                """;

        User user = User.builder().id("u1").name("Alice").email("alice@test.com")
                .phone("123").role(UserRole.CUSTOMER).isActive(true).build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        userSyncConsumer.handleSyncRequest(message);

        ArgumentCaptor<UserEvent> captor = ArgumentCaptor.forClass(UserEvent.class);
        verify(eventPublisher).publishUserEvent(eq("USER_SYNC"), captor.capture());

        UserEvent published = captor.getValue();
        assertThat(published.getUserId()).isEqualTo("u1");
        assertThat(published.getName()).isEqualTo("Alice");
        assertThat(published.getRole()).isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("USER_SYNC_REQUEST for non-existent user does not publish")
    void handleSyncRequest_userNotFound_doesNotPublish() {
        String message = """
                {"requestType":"USER_SYNC_REQUEST","userId":"unknown","timestamp":"2025-01-01T00:00:00Z"}
                """;

        when(userRepository.findById("unknown")).thenReturn(Optional.empty());

        userSyncConsumer.handleSyncRequest(message);

        verify(eventPublisher, never()).publishUserEvent(any(), any());
    }

    @Test
    @DisplayName("Unknown request type does not throw")
    void handleSyncRequest_unknownType_doesNotThrow() {
        String message = """
                {"requestType":"UNKNOWN_TYPE","timestamp":"2025-01-01T00:00:00Z"}
                """;

        userSyncConsumer.handleSyncRequest(message);

        verify(eventPublisher, never()).publishUserEvent(any(), any());
    }

    @Test
    @DisplayName("Invalid JSON does not throw")
    void handleSyncRequest_invalidJson_doesNotThrow() {
        userSyncConsumer.handleSyncRequest("not valid json");

        verify(eventPublisher, never()).publishUserEvent(any(), any());
    }

    @Test
    @DisplayName("Missing requestType does not throw")
    void handleSyncRequest_missingRequestType_doesNotThrow() {
        String message = """
                {"timestamp":"2025-01-01T00:00:00Z"}
                """;

        userSyncConsumer.handleSyncRequest(message);

        verify(eventPublisher, never()).publishUserEvent(any(), any());
    }
}
