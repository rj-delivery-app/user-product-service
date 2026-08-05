package com.example.userproduct.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Why: Represents user domain events published to Kafka for downstream services.
 * What: Contains user fields that other services need when a user is created or updated.
 * Test: Assert serialization to JSON includes all fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEvent {
    private String userId;
    private String name;
    private String email;
    private String phone;
    private String role;
    private Boolean isActive;
}
