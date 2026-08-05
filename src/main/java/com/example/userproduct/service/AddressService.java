package com.example.userproduct.service;

import com.example.userproduct.dao.AddressRepository;
import com.example.userproduct.dao.UserRepository;
import com.example.userproduct.dto.AddressRequest;
import com.example.userproduct.dto.AddressResponse;
import com.example.userproduct.entities.Address;
import com.example.userproduct.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for address management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    /**
     * Get all addresses for a user
     */
    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(String userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a single address by ID (with ownership check)
     */
    @Transactional(readOnly = true)
    public AddressResponse getAddress(String addressId, String userId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));
        return mapToResponse(address);
    }

    /**
     * Add a new address for the current user
     */
    @Transactional
    public AddressResponse createAddress(String userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Address address = new Address();
        address.setAddressType(request.getAddressType());
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setZipCode(request.getZipCode());
        address.setUser(user);

        address = addressRepository.save(address);
        log.info("Address created: {} for user: {}", address.getId(), userId);

        return mapToResponse(address);
    }

    /**
     * Update an existing address (with ownership check)
     */
    @Transactional
    public AddressResponse updateAddress(String addressId, String userId, AddressRequest request) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));

        address.setAddressType(request.getAddressType());
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setZipCode(request.getZipCode());

        address = addressRepository.save(address);
        log.info("Address updated: {} for user: {}", addressId, userId);

        return mapToResponse(address);
    }

    /**
     * Delete an address (with ownership check)
     */
    @Transactional
    public void deleteAddress(String addressId, String userId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));

        addressRepository.delete(address);
        log.info("Address deleted: {} for user: {}", addressId, userId);
    }

    /**
     * Map Address entity to AddressResponse DTO
     */
    private AddressResponse mapToResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .addressType(address.getAddressType().name())
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .build();
    }
}
