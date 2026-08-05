package com.example.userproduct.aop;

import com.example.userproduct.dao.ProductRepository;
import com.example.userproduct.dao.UserRepository;
import com.example.userproduct.dto.AuthResponse;
import com.example.userproduct.dto.ProductResponse;
import com.example.userproduct.dto.UserProfileResponse;
import com.example.userproduct.entities.Product;
import com.example.userproduct.entities.User;
import com.example.userproduct.events.EventPublisher;
import com.example.userproduct.events.ProductEvent;
import com.example.userproduct.events.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Why: Centralizes Kafka event publishing as a cross-cutting concern so service
 *      methods remain focused on business logic without explicit event calls.
 * What: Intercepts methods annotated with @PublishKafkaEvent, executes them, and
 *       publishes the appropriate Kafka event on successful completion.
 * Test: Mock EventPublisher and repositories; verify correct event type and payload
 *       are published for product and user operations, including void (delete) methods.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventAspect {

    private final EventPublisher eventPublisher;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Intercepts @PublishKafkaEvent-annotated methods, executes them, and publishes
     * the corresponding Kafka event after successful completion.
     *
     * Uses @Around to handle the delete case where entity data must be captured
     * before the method removes it from the database.
     */
    @Around("@annotation(annotation)")
    public Object publishEvent(ProceedingJoinPoint joinPoint, PublishKafkaEvent annotation) throws Throwable {
        String topic = annotation.topic();
        String eventType = annotation.eventType();

        // For delete operations, capture entity data before method execution
        // because the entity will no longer exist after deletion.
        ProductEvent preDeleteEvent = null;
        if ("product".equals(topic) && eventType.contains("DELETED")) {
            preDeleteEvent = buildProductEventFromArgs(joinPoint);
        }

        Object result = joinPoint.proceed();

        if (preDeleteEvent != null) {
            eventPublisher.publishProductEvent(eventType, preDeleteEvent);
        } else if ("product".equals(topic)) {
            publishProductEvent(eventType, result);
        } else if ("user".equals(topic)) {
            publishUserEvent(eventType, result, joinPoint);
        }

        return result;
    }

    /**
     * Build a ProductEvent by looking up the product from the repository using
     * the productId extracted from the method's first argument.
     */
    private ProductEvent buildProductEventFromArgs(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String productId = (String) args[0];
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found for event publishing"));
        return buildProductEvent(product);
    }

    /**
     * Publish a product event by extracting data from the ProductResponse return value.
     */
    private void publishProductEvent(String eventType, Object result) {
        if (result instanceof ProductResponse response) {
            ProductEvent event = ProductEvent.builder()
                    .productId(response.getId())
                    .merchantId(response.getMerchantId())
                    .name(response.getName())
                    .price(response.getPrice())
                    .category(response.getCategory())
                    .isAvailable(response.getIsAvailable())
                    .build();
            eventPublisher.publishProductEvent(eventType, event);
        } else {
            log.warn("Unexpected return type for product event: {}", result != null ? result.getClass() : "null");
        }
    }

    /**
     * Publish a user event by extracting data from the return value.
     * Handles both UserProfileResponse (from UserService) and AuthResponse (from AuthService).
     */
    private void publishUserEvent(String eventType, Object result, ProceedingJoinPoint joinPoint) {
        if (result instanceof UserProfileResponse response) {
            UserEvent event = UserEvent.builder()
                    .userId(response.getId())
                    .name(response.getName())
                    .email(response.getEmail())
                    .phone(response.getPhone())
                    .role(response.getRole())
                    .isActive(response.getIsActive())
                    .build();
            eventPublisher.publishUserEvent(eventType, event);
        } else if (result instanceof AuthResponse response) {
            // For registration, look up the full user entity to get all fields
            User user = userRepository.findById(response.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found for event publishing"));
            UserEvent event = UserEvent.builder()
                    .userId(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .role(user.getRole().name())
                    .isActive(user.getIsActive())
                    .build();
            eventPublisher.publishUserEvent(eventType, event);
        } else {
            log.warn("Unexpected return type for user event: {}", result != null ? result.getClass() : "null");
        }
    }

    /**
     * Build a ProductEvent from a Product entity.
     */
    private ProductEvent buildProductEvent(Product product) {
        return ProductEvent.builder()
                .productId(product.getId())
                .merchantId(product.getMerchant().getId())
                .name(product.getName())
                .price(product.getPrice())
                .category(product.getCategory())
                .isAvailable(product.getIsAvailable())
                .build();
    }
}
