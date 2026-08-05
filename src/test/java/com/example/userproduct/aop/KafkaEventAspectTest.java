package com.example.userproduct.aop;

import com.example.userproduct.dao.ProductRepository;
import com.example.userproduct.dao.UserRepository;
import com.example.userproduct.dto.AuthResponse;
import com.example.userproduct.dto.ProductResponse;
import com.example.userproduct.dto.UserProfileResponse;
import com.example.userproduct.entities.Product;
import com.example.userproduct.entities.User;
import com.example.userproduct.entities.UserRole;
import com.example.userproduct.events.EventPublisher;
import com.example.userproduct.events.ProductEvent;
import com.example.userproduct.events.UserEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Why: Validates that KafkaEventAspect correctly intercepts annotated methods and
 *      publishes the appropriate Kafka events with correct payloads.
 * What: Unit tests for each event publishing scenario (product CRUD, user update,
 *       user registration) including error and void-return cases.
 * Test: Mock ProceedingJoinPoint and EventPublisher; verify event type, topic, and
 *       payload fields for each scenario.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaEventAspect Tests")
class KafkaEventAspectTest {

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @InjectMocks
    private KafkaEventAspect kafkaEventAspect;

    private ProductResponse productResponse;
    private UserProfileResponse userProfileResponse;
    private AuthResponse authResponse;
    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        User merchant = User.builder()
                .id("merchant-1")
                .name("Merchant One")
                .email("merchant@test.com")
                .role(UserRole.MERCHANT_ADMIN)
                .build();

        product = Product.builder()
                .id("product-1")
                .merchant(merchant)
                .name("Pizza")
                .price(new BigDecimal("12.99"))
                .category("Italian")
                .isAvailable(true)
                .build();

        productResponse = ProductResponse.builder()
                .id("product-1")
                .merchantId("merchant-1")
                .merchantName("Merchant One")
                .name("Pizza")
                .price(new BigDecimal("12.99"))
                .category("Italian")
                .isAvailable(true)
                .build();

        user = User.builder()
                .id("user-1")
                .name("John Doe")
                .email("john@example.com")
                .phone("1234567890")
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();

        userProfileResponse = UserProfileResponse.builder()
                .id("user-1")
                .name("John Doe")
                .email("john@example.com")
                .phone("1234567890")
                .role("CUSTOMER")
                .isActive(true)
                .build();

        authResponse = AuthResponse.builder()
                .token("jwt-token")
                .userId("user-1")
                .email("john@example.com")
                .name("John Doe")
                .role("CUSTOMER")
                .build();
    }

    @Nested
    @DisplayName("Product events")
    class ProductEvents {

        @Test
        @DisplayName("Publishes PRODUCT_CREATED event from ProductResponse return value")
        void publishEvent_productCreated_publishesCorrectEvent() throws Throwable {
            PublishKafkaEvent annotation = createAnnotation("PRODUCT_CREATED", "product");
            when(joinPoint.proceed()).thenReturn(productResponse);

            Object result = kafkaEventAspect.publishEvent(joinPoint, annotation);

            assertThat(result).isEqualTo(productResponse);

            ArgumentCaptor<ProductEvent> captor = ArgumentCaptor.forClass(ProductEvent.class);
            verify(eventPublisher).publishProductEvent(eq("PRODUCT_CREATED"), captor.capture());

            ProductEvent event = captor.getValue();
            assertThat(event.getProductId()).isEqualTo("product-1");
            assertThat(event.getMerchantId()).isEqualTo("merchant-1");
            assertThat(event.getName()).isEqualTo("Pizza");
            assertThat(event.getPrice()).isEqualByComparingTo(new BigDecimal("12.99"));
            assertThat(event.getCategory()).isEqualTo("Italian");
            assertThat(event.getIsAvailable()).isTrue();
        }

        @Test
        @DisplayName("Publishes PRODUCT_UPDATED event from ProductResponse return value")
        void publishEvent_productUpdated_publishesCorrectEvent() throws Throwable {
            PublishKafkaEvent annotation = createAnnotation("PRODUCT_UPDATED", "product");
            when(joinPoint.proceed()).thenReturn(productResponse);

            kafkaEventAspect.publishEvent(joinPoint, annotation);

            verify(eventPublisher).publishProductEvent(eq("PRODUCT_UPDATED"), any(ProductEvent.class));
        }

        @Test
        @DisplayName("Publishes PRODUCT_DELETED event using pre-fetched entity data")
        void publishEvent_productDeleted_fetchesEntityBeforeDelete() throws Throwable {
            PublishKafkaEvent annotation = createAnnotation("PRODUCT_DELETED", "product");
            when(joinPoint.getArgs()).thenReturn(new Object[]{"product-1", "merchant-1"});
            when(productRepository.findById("product-1")).thenReturn(Optional.of(product));
            when(joinPoint.proceed()).thenReturn(null);

            Object result = kafkaEventAspect.publishEvent(joinPoint, annotation);

            assertThat(result).isNull();

            ArgumentCaptor<ProductEvent> captor = ArgumentCaptor.forClass(ProductEvent.class);
            verify(eventPublisher).publishProductEvent(eq("PRODUCT_DELETED"), captor.capture());

            ProductEvent event = captor.getValue();
            assertThat(event.getProductId()).isEqualTo("product-1");
            assertThat(event.getMerchantId()).isEqualTo("merchant-1");
            assertThat(event.getName()).isEqualTo("Pizza");
        }

        @Test
        @DisplayName("PRODUCT_DELETED throws when product not found in repository")
        void publishEvent_productDeleted_notFound_throws() throws Throwable {
            PublishKafkaEvent annotation = createAnnotation("PRODUCT_DELETED", "product");
            when(joinPoint.getArgs()).thenReturn(new Object[]{"nonexistent", "merchant-1"});
            when(productRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> kafkaEventAspect.publishEvent(joinPoint, annotation))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Product not found for event publishing");
        }
    }

    @Nested
    @DisplayName("User events")
    class UserEvents {

        @Test
        @DisplayName("Publishes USER_UPDATED event from UserProfileResponse return value")
        void publishEvent_userUpdated_publishesCorrectEvent() throws Throwable {
            PublishKafkaEvent annotation = createAnnotation("USER_UPDATED", "user");
            when(joinPoint.proceed()).thenReturn(userProfileResponse);

            Object result = kafkaEventAspect.publishEvent(joinPoint, annotation);

            assertThat(result).isEqualTo(userProfileResponse);

            ArgumentCaptor<UserEvent> captor = ArgumentCaptor.forClass(UserEvent.class);
            verify(eventPublisher).publishUserEvent(eq("USER_UPDATED"), captor.capture());

            UserEvent event = captor.getValue();
            assertThat(event.getUserId()).isEqualTo("user-1");
            assertThat(event.getName()).isEqualTo("John Doe");
            assertThat(event.getEmail()).isEqualTo("john@example.com");
            assertThat(event.getPhone()).isEqualTo("1234567890");
            assertThat(event.getRole()).isEqualTo("CUSTOMER");
            assertThat(event.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("Publishes USER_CREATED event from AuthResponse return value")
        void publishEvent_userCreated_fromAuthResponse_publishesCorrectEvent() throws Throwable {
            PublishKafkaEvent annotation = createAnnotation("USER_CREATED", "user");
            when(joinPoint.proceed()).thenReturn(authResponse);
            when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

            Object result = kafkaEventAspect.publishEvent(joinPoint, annotation);

            assertThat(result).isEqualTo(authResponse);

            ArgumentCaptor<UserEvent> captor = ArgumentCaptor.forClass(UserEvent.class);
            verify(eventPublisher).publishUserEvent(eq("USER_CREATED"), captor.capture());

            UserEvent event = captor.getValue();
            assertThat(event.getUserId()).isEqualTo("user-1");
            assertThat(event.getName()).isEqualTo("John Doe");
            assertThat(event.getEmail()).isEqualTo("john@example.com");
            assertThat(event.getPhone()).isEqualTo("1234567890");
            assertThat(event.getRole()).isEqualTo("CUSTOMER");
            assertThat(event.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("USER_CREATED from AuthResponse throws when user not found")
        void publishEvent_userCreated_userNotFound_throws() throws Throwable {
            PublishKafkaEvent annotation = createAnnotation("USER_CREATED", "user");
            when(joinPoint.proceed()).thenReturn(authResponse);
            when(userRepository.findById("user-1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> kafkaEventAspect.publishEvent(joinPoint, annotation))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User not found for event publishing");
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("Does not publish event when method throws exception")
        void publishEvent_methodThrows_noEventPublished() throws Throwable {
            PublishKafkaEvent annotation = createAnnotation("PRODUCT_CREATED", "product");
            when(joinPoint.proceed()).thenThrow(new IllegalArgumentException("Merchant not found"));

            assertThatThrownBy(() -> kafkaEventAspect.publishEvent(joinPoint, annotation))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Merchant not found");

            verify(eventPublisher, never()).publishProductEvent(any(), any());
            verify(eventPublisher, never()).publishUserEvent(any(), any());
        }

        @Test
        @DisplayName("Returns original method result unchanged")
        void publishEvent_returnsOriginalResult() throws Throwable {
            PublishKafkaEvent annotation = createAnnotation("PRODUCT_UPDATED", "product");
            when(joinPoint.proceed()).thenReturn(productResponse);

            Object result = kafkaEventAspect.publishEvent(joinPoint, annotation);

            assertThat(result).isSameAs(productResponse);
        }
    }

    /**
     * Helper to create a PublishKafkaEvent annotation proxy for testing.
     */
    private PublishKafkaEvent createAnnotation(String eventType, String topic) {
        return new PublishKafkaEvent() {
            @Override
            public String eventType() {
                return eventType;
            }

            @Override
            public String topic() {
                return topic;
            }

            @Override
            public Class<PublishKafkaEvent> annotationType() {
                return PublishKafkaEvent.class;
            }
        };
    }
}
