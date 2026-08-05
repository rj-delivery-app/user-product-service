package com.example.userproduct.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Why: Decouples Kafka event publishing from business logic so service methods stay
 *      focused on domain operations instead of mixing in cross-cutting event concerns.
 * What: Marks a service method for automatic Kafka event publishing via AOP after
 *       successful execution.
 * Test: Annotate a service method, invoke it, and verify the aspect publishes the
 *       correct event type to the correct topic.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PublishKafkaEvent {

    /**
     * The event type string, e.g. "PRODUCT_CREATED", "USER_UPDATED".
     */
    String eventType();

    /**
     * The topic category: "product" or "user". Determines which event DTO and
     * Kafka topic the aspect uses.
     */
    String topic();
}
