package com.example.userproduct.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Why: Represents product domain events published to Kafka for downstream services.
 * What: Contains product fields that other services need when a product is created, updated, or deleted.
 * Test: Assert serialization to JSON includes all fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEvent {
    private String productId;
    private String merchantId;
    private String name;
    private BigDecimal price;
    private String category;
    private Boolean isAvailable;
}
