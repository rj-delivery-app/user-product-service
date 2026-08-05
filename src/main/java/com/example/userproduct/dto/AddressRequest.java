package com.example.userproduct.dto;

import com.example.userproduct.entities.AddressType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for address creation and update requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Address request")
public class AddressRequest {

    @Schema(description = "Address type", example = "HOME")
    @NotNull(message = "Address type is required")
    private AddressType addressType;

    @Schema(description = "Street address", example = "123 Main St")
    @NotBlank(message = "Street is required")
    @Size(max = 255, message = "Street must be at most 255 characters")
    private String street;

    @Schema(description = "City", example = "New York")
    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must be at most 100 characters")
    private String city;

    @Schema(description = "State", example = "NY")
    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State must be at most 100 characters")
    private String state;

    @Schema(description = "ZIP code", example = "10001")
    @NotBlank(message = "ZIP code is required")
    @Size(max = 20, message = "ZIP code must be at most 20 characters")
    private String zipCode;
}
