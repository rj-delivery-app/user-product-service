package com.example.userproduct.dao;

import com.example.userproduct.entities.User;
import com.example.userproduct.entities.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);
    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);
    /**
     * Find all users by role
     */
    List<User> findByRole(UserRole role);
    /**
     * Find active riders
     */
    List<User> findByRoleAndIsActiveTrue(UserRole role);
}
