package com.example.userproduct.dao;

import com.example.userproduct.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Product entity
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    /**
     * Find all products by merchant ID
     */
    List<Product> findByMerchantId(String merchantId);

    /**
     * Find available products by merchant
     */
    List<Product> findByMerchantIdAndIsAvailableTrue(String merchantId);

    /**
     * Find products by category
     */
    List<Product> findByCategory(String category);

    /**
     * Find available products by category
     */
    List<Product> findByCategoryAndIsAvailableTrue(String category);

    /**
     * Find all available products with pagination
     */
    Page<Product> findByIsAvailableTrue(Pageable pageable);

    /**
     * Find products by merchant ID with pagination
     */
    Page<Product> findByMerchantId(String merchantId, Pageable pageable);

    /**
     * Find products by category with pagination
     */
    Page<Product> findByCategory(String category, Pageable pageable);

    /**
     * Search products by name or description (case-insensitive)
     */
    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String name, String description, Pageable pageable);

    /**
     * Find all distinct categories
     */
    @Query("SELECT DISTINCT p.category FROM Product p ORDER BY p.category")
    List<String> findDistinctCategories();
}
