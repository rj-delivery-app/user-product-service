package com.example.userproduct.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventPublisher Tests")
class EventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private EventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        eventPublisher = new EventPublisher(kafkaTemplate, new ObjectMapper());
    }

    @Test
    @DisplayName("publishUserEvent sends to user.events topic with correct key")
    void publishUserEvent_sendsToCorrectTopic() {
        UserEvent event = UserEvent.builder()
                .userId("user-1").name("John").email("john@test.com")
                .phone("1234567890").role("CUSTOMER").isActive(true).build();

        eventPublisher.publishUserEvent("USER_CREATED", event);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("user.events"), eq("user-1"), jsonCaptor.capture());

        String json = jsonCaptor.getValue();
        assertThat(json).contains("\"eventType\":\"USER_CREATED\"");
        assertThat(json).contains("\"eventId\"");
        assertThat(json).contains("\"timestamp\"");
        assertThat(json).contains("\"userId\":\"user-1\"");
    }

    @Test
    @DisplayName("publishProductEvent sends to product.events topic with correct key")
    void publishProductEvent_sendsToCorrectTopic() {
        ProductEvent event = ProductEvent.builder()
                .productId("prod-1").merchantId("merchant-1").name("Pizza")
                .price(new BigDecimal("12.99")).category("Italian").isAvailable(true).build();

        eventPublisher.publishProductEvent("PRODUCT_CREATED", event);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("product.events"), eq("prod-1"), jsonCaptor.capture());

        String json = jsonCaptor.getValue();
        assertThat(json).contains("\"eventType\":\"PRODUCT_CREATED\"");
        assertThat(json).contains("\"productId\":\"prod-1\"");
        assertThat(json).contains("\"merchantId\":\"merchant-1\"");
    }

    @Test
    @DisplayName("publishUserEvent for USER_UPDATED sends correct event type")
    void publishUserEvent_updatedType() {
        UserEvent event = UserEvent.builder().userId("user-1").name("Jane").email("jane@test.com")
                .phone("0987654321").role("CUSTOMER").isActive(true).build();

        eventPublisher.publishUserEvent("USER_UPDATED", event);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("user.events"), eq("user-1"), jsonCaptor.capture());
        assertThat(jsonCaptor.getValue()).contains("\"eventType\":\"USER_UPDATED\"");
    }

    @Test
    @DisplayName("publishProductEvent for PRODUCT_DELETED sends correct event type")
    void publishProductEvent_deletedType() {
        ProductEvent event = ProductEvent.builder().productId("prod-1").merchantId("merchant-1")
                .name("Pizza").price(new BigDecimal("12.99")).category("Italian").isAvailable(true).build();

        eventPublisher.publishProductEvent("PRODUCT_DELETED", event);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("product.events"), eq("prod-1"), jsonCaptor.capture());
        assertThat(jsonCaptor.getValue()).contains("\"eventType\":\"PRODUCT_DELETED\"");
    }
}
