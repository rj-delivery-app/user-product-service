package com.example.userproduct.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: Centralizes domain event publishing to Kafka, ensuring a consistent envelope
 *      format across all event types (user and product events).
 * What: Wraps event payloads in a standard envelope with eventId, eventType, timestamp,
 *       and payload, then sends to the appropriate Kafka topic.
 * Test: Mock KafkaTemplate and verify send() is called with correct topic and JSON envelope.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private static final String USER_EVENTS_TOPIC = "user.events";
    private static final String PRODUCT_EVENTS_TOPIC = "product.events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publish a user domain event to the user.events topic.
     */
    public void publishUserEvent(String eventType, UserEvent payload) {
        String json = buildEnvelope(eventType, payload);
        kafkaTemplate.send(USER_EVENTS_TOPIC, payload.getUserId(), json);
        log.info("Published {} event for user: {}", eventType, payload.getUserId());
    }

    /**
     * Publish a product domain event to the product.events topic.
     */
    public void publishProductEvent(String eventType, ProductEvent payload) {
        String json = buildEnvelope(eventType, payload);
        kafkaTemplate.send(PRODUCT_EVENTS_TOPIC, payload.getProductId(), json);
        log.info("Published {} event for product: {}", eventType, payload.getProductId());
    }

    /**
     * Build a standard event envelope wrapping the given payload.
     */
    private String buildEnvelope(String eventType, Object payload) {
        try {
            Map<String, Object> envelope = new HashMap<>();
            envelope.put("eventId", UUID.randomUUID().toString());
            envelope.put("eventType", eventType);
            envelope.put("timestamp", Instant.now().toString());
            envelope.put("payload", payload);
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
