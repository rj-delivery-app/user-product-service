package com.example.userproduct.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for address response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponse {
    private String id;
    private String addressType;
    private String street;
    private String city;
    private String state;
    private String zipCode;
}
