package com.example.userproduct.dao;

import com.example.userproduct.entities.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Address entity
 */
@Repository
public interface AddressRepository extends JpaRepository<Address, String> {

    /**
     * Find all addresses by user ID
     */
    List<Address> findByUserId(String userId);

    /**
     * Find address by ID and user ID (ownership check)
     */
    Optional<Address> findByIdAndUserId(String id, String userId);
}
