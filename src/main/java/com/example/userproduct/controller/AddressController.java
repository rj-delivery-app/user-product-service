package com.example.userproduct.controller;

import com.example.userproduct.dto.AddressRequest;
import com.example.userproduct.dto.AddressResponse;
import com.example.userproduct.service.AddressService;
import com.example.userproduct.service.UserContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for address management endpoints
 */
@RestController
@RequestMapping("/api/users/addresses")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Addresses", description = "User address management endpoints")
public class AddressController {

    private final AddressService addressService;
    private final UserContextService userContextService;

    @GetMapping
    @Operation(summary = "Get current user's addresses",
        description = "Get all addresses for the currently authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AddressResponse.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<AddressResponse>> getMyAddresses() {
        String userId = userContextService.getCurrentUserId();
        List<AddressResponse> response = addressService.getUserAddresses(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get address by ID",
        description = "Get a specific address by ID. Only the owner can access their addresses.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Address found",
            content = @Content(schema = @Schema(implementation = AddressResponse.class))),
        @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<AddressResponse> getAddress(
            @Parameter(description = "Address ID", required = true) @PathVariable String id) {
        String userId = userContextService.getCurrentUserId();
        AddressResponse response = addressService.getAddress(id, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Add new address",
        description = "Add a new address for the currently authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Address created successfully",
            content = @Content(schema = @Schema(implementation = AddressResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<AddressResponse> createAddress(@Valid @RequestBody AddressRequest request) {
        String userId = userContextService.getCurrentUserId();
        AddressResponse response = addressService.createAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update address",
        description = "Update an existing address. Only the owner can update their addresses.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Address updated successfully",
            content = @Content(schema = @Schema(implementation = AddressResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<AddressResponse> updateAddress(
            @Parameter(description = "Address ID", required = true) @PathVariable String id,
            @Valid @RequestBody AddressRequest request) {
        String userId = userContextService.getCurrentUserId();
        AddressResponse response = addressService.updateAddress(id, userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete address",
        description = "Delete an address. Only the owner can delete their addresses.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Address deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<Void> deleteAddress(
            @Parameter(description = "Address ID", required = true) @PathVariable String id) {
        String userId = userContextService.getCurrentUserId();
        addressService.deleteAddress(id, userId);
        return ResponseEntity.noContent().build();
    }
}
