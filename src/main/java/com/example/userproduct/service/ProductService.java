package com.example.userproduct.service;

import com.example.userproduct.aop.PublishKafkaEvent;
import com.example.userproduct.dao.ProductRepository;
import com.example.userproduct.dao.UserRepository;
import com.example.userproduct.dto.CreateProductRequest;
import com.example.userproduct.dto.ProductResponse;
import com.example.userproduct.dto.UpdateProductRequest;
import com.example.userproduct.entities.Product;
import com.example.userproduct.entities.User;
import com.example.userproduct.entities.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for product management operations.
 *
 * Why: Encapsulates product CRUD logic; Kafka events are published declaratively
 *      via @PublishKafkaEvent annotations handled by KafkaEventAspect.
 * What: Manages product creation, update, deletion, search, and category listing.
 * Test: Mock ProductRepository; verify ownership checks and correct return values.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Create a new product (merchant only)
     */
    @Transactional()
    @PublishKafkaEvent(eventType = "PRODUCT_CREATED", topic = "product")
    public ProductResponse createProduct(String merchantId, CreateProductRequest request) {
        User merchant = userRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));

        if (merchant.getRole() != UserRole.MERCHANT_ADMIN) {
            throw new IllegalArgumentException("Only merchants can create products");
        }

        Product product = Product.builder()
                .merchant(merchant)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .category(request.getCategory())
                .isAvailable(true)
                .build();

        product = productRepository.save(product);
        log.info("Product created: {} by merchant: {}", product.getId(), merchantId);

        return mapToResponse(product);
    }

    /**
     * Update a product (owner merchant only)
     */
    @Transactional
    @PublishKafkaEvent(eventType = "PRODUCT_UPDATED", topic = "product")
    public ProductResponse updateProduct(String productId, String merchantId, UpdateProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        validateOwnership(product, merchantId);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setCategory(request.getCategory());

        product = productRepository.save(product);
        log.info("Product updated: {} by merchant: {}", productId, merchantId);

        return mapToResponse(product);
    }

    /**
     * Delete a product (owner merchant only)
     */
    @Transactional
    @PublishKafkaEvent(eventType = "PRODUCT_DELETED", topic = "product")
    public void deleteProduct(String productId, String merchantId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        validateOwnership(product, merchantId);

        productRepository.delete(product);
        log.info("Product deleted: {} by merchant: {}", productId, merchantId);
    }

    /**
     * Get product by ID
     */
    @Transactional(readOnly = true)
    public ProductResponse getProduct(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        return mapToResponse(product);
    }

    /**
     * List all available products with pagination
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findByIsAvailableTrue(pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get products by merchant with pagination
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByMerchant(String merchantId, Pageable pageable) {
        return productRepository.findByMerchantId(merchantId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get products by category with pagination
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategory(String category, Pageable pageable) {
        return productRepository.findByCategory(category, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Toggle product availability (owner merchant only)
     */
    @Transactional
    @PublishKafkaEvent(eventType = "PRODUCT_UPDATED", topic = "product")
    public ProductResponse toggleAvailability(String productId, String merchantId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        validateOwnership(product, merchantId);

        product.setIsAvailable(!product.getIsAvailable());
        product = productRepository.save(product);
        log.info("Product {} availability toggled to {} by merchant: {}",
                productId, product.getIsAvailable(), merchantId);

        return mapToResponse(product);
    }

    /**
     * Search products by name or description
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String query, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        query, query, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get all distinct categories
     */
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return productRepository.findDistinctCategories();
    }

    /**
     * Validate that the merchant owns the product
     */
    private void validateOwnership(Product product, String merchantId) {
        if (!product.getMerchant().getId().equals(merchantId)) {
            throw new IllegalArgumentException("You can only manage your own products");
        }
    }

    /**
     * Map Product entity to ProductResponse DTO
     */
    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .merchantId(product.getMerchant().getId())
                .merchantName(product.getMerchant().getName())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .category(product.getCategory())
                .isAvailable(product.getIsAvailable())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
