package com.example.userproduct.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Why: Provides a lightweight user representation for internal service-to-service calls,
 *      avoiding exposure of sensitive fields like password.
 * What: Contains only the fields other microservices need to process orders and deliveries.
 * Test: Assert that mapping from User entity populates all fields correctly.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInternalResponse {
    private String id;
    private String name;
    private String email;
    private String role;
    private Boolean isActive;
}
