package com.example.userproduct.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Why: Provides address data for internal service-to-service calls (e.g., order service
 *      needs delivery address details).
 * What: Contains address fields needed for delivery processing.
 * Test: Assert that mapping from Address entity populates all fields correctly.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressInternalResponse {
    private String id;
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private String type;
}
