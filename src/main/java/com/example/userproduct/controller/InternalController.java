package com.example.userproduct.controller;

import com.example.userproduct.dao.AddressRepository;
import com.example.userproduct.dao.ProductRepository;
import com.example.userproduct.dao.UserRepository;
import com.example.userproduct.dto.AddressInternalResponse;
import com.example.userproduct.dto.ProductInternalResponse;
import com.example.userproduct.dto.UserInternalResponse;
import com.example.userproduct.entities.Address;
import com.example.userproduct.entities.Product;
import com.example.userproduct.entities.User;
import com.example.userproduct.entities.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Why: Provides internal service-to-service endpoints that bypass JWT authentication,
 *      allowing the order-service and delivery-service to look up user, product, and
 *      address data without a user token.
 * What: Exposes lightweight GET endpoints under /internal/** for user, address, product,
 *       and batch product lookups.
 * Test: Call /internal/users/{id} without auth and assert 200 with correct JSON fields.
 */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;

    /**
     * Get user by ID for internal service-to-service calls.
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<UserInternalResponse> getUserById(@PathVariable String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserInternalResponse response = UserInternalResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific address for a user (internal service-to-service).
     */
    @GetMapping("/users/{userId}/addresses/{addressId}")
    public ResponseEntity<AddressInternalResponse> getUserAddress(
            @PathVariable String userId,
            @PathVariable String addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));

        AddressInternalResponse response = AddressInternalResponse.builder()
                .id(address.getId())
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .type(address.getAddressType().name())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get all active delivery riders for internal service-to-service calls.
     */
    @GetMapping("/riders")
    public ResponseEntity<List<UserInternalResponse>> getActiveRiders() {
        List<User> riders = userRepository.findByRoleAndIsActiveTrue(UserRole.DELIVERY_RIDER);
        return ResponseEntity.ok(riders.stream().map(this::mapUserToInternal).toList());
    }

    /**
     * Get product by ID for internal service-to-service calls.
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductInternalResponse> getProductById(@PathVariable String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        ProductInternalResponse response = mapProductToInternal(product);

        return ResponseEntity.ok(response);
    }

    /**
     * Get multiple products by IDs for internal service-to-service calls (batch lookup).
     */
    @GetMapping("/products/batch")
    public ResponseEntity<List<ProductInternalResponse>> getProductsByIds(
            @RequestParam List<String> ids) {
        List<ProductInternalResponse> responses = productRepository.findAllById(ids).stream()
                .map(this::mapProductToInternal)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    private UserInternalResponse mapUserToInternal(User user) {
        return UserInternalResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .build();
    }

    private ProductInternalResponse mapProductToInternal(Product product) {
        return ProductInternalResponse.builder()
                .id(product.getId())
                .merchantId(product.getMerchant().getId())
                .name(product.getName())
                .price(product.getPrice())
                .isAvailable(product.getIsAvailable())
                .build();
    }
}
