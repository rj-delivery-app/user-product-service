package com.example.userproduct.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Why: Provides a lightweight product representation for internal service-to-service calls.
 * What: Contains product fields needed by the order service to validate and process orders.
 * Test: Assert that mapping from Product entity populates all fields correctly.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductInternalResponse {
    private String id;
    private String merchantId;
    private String name;
    private BigDecimal price;
    private Boolean isAvailable;
}
