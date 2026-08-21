package com.example.userproduct.controller;

import com.example.userproduct.dto.CreateProductRequest;
import com.example.userproduct.dto.ImageHolder;
import com.example.userproduct.dto.ProductResponse;
import com.example.userproduct.dto.UpdateProductRequest;
import com.example.userproduct.service.FileService;
import com.example.userproduct.service.ProductService;
import com.example.userproduct.service.UserContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

/**
 * Controller for product management endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Products", description = "Product management endpoints")
public class ProductController {

    private final ProductService productService;
    private final UserContextService userContextService;
    private final ImageHolder imageHolder;
    private final FileService fileService;

    @PostMapping
    @PreAuthorize("hasRole('MERCHANT_ADMIN')")
    @Operation(summary = "Create product", description = "Create a new product. Requires MERCHANT_ADMIN role.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Product created successfully",
            content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        if (imageHolder == null || imageHolder.getFileBytes() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is empty from controller");
        }
        String merchantId = userContextService.getCurrentUserId();
        ProductResponse response = productService.createProduct(merchantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MERCHANT_ADMIN')")
    @Operation(summary = "Update product", description = "Update an existing product. Only the owner merchant can update.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product updated successfully",
            content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "Product ID", required = true) @PathVariable String id,
            @Valid @RequestBody UpdateProductRequest request) {
        String merchantId = userContextService.getCurrentUserId();
        ProductResponse response = productService.updateProduct(id, merchantId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MERCHANT_ADMIN')")
    @Operation(summary = "Delete product", description = "Delete a product. Only the owner merchant can delete.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product ID", required = true) @PathVariable String id) {
        String merchantId = userContextService.getCurrentUserId();
        productService.deleteProduct(id, merchantId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Get product details by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product found",
            content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> getProduct(
            @Parameter(description = "Product ID", required = true) @PathVariable String id) {
        ProductResponse response = productService.getProduct(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List all products", description = "List all available products with pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    })
    public ResponseEntity<Page<ProductResponse>> getAllProducts(Pageable pageable) {
        Page<ProductResponse> response = productService.getAllProducts(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "Get products by merchant", description = "Get all products for a specific merchant with pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    })
    public ResponseEntity<Page<ProductResponse>> getProductsByMerchant(
            @Parameter(description = "Merchant ID", required = true) @PathVariable String merchantId,
            Pageable pageable) {
        Page<ProductResponse> response = productService.getProductsByMerchant(merchantId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get products by category", description = "Get all products in a specific category with pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    })
    public ResponseEntity<Page<ProductResponse>> getProductsByCategory(
            @Parameter(description = "Category name", required = true) @PathVariable String category,
            Pageable pageable) {
        Page<ProductResponse> response = productService.getProductsByCategory(category, pageable);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasRole('MERCHANT_ADMIN')")
    @Operation(summary = "Toggle product availability",
        description = "Toggle a product's availability status. Only the owner merchant can toggle.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product availability toggled",
            content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> toggleAvailability(
            @Parameter(description = "Product ID", required = true) @PathVariable String id) {
        String merchantId = userContextService.getCurrentUserId();
        ProductResponse response = productService.toggleAvailability(id, merchantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search products", description = "Search products by name or description (case-insensitive)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
    })
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @Parameter(description = "Search query", required = true) @RequestParam String query,
            Pageable pageable) {
        Page<ProductResponse> response = productService.searchProducts(query, pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    @Operation(summary = "upload products image", description = "image")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "upload")
    })
    public ResponseEntity<String> uploadImage(
            @RequestParam("image") MultipartFile file,
            @RequestParam("productName") String productName
    ) {
        try {
            imageHolder.setContentType(file.getContentType());
            imageHolder.setOriginalFileName(file.getOriginalFilename());
            imageHolder.setFileBytes(file.getBytes());
            fileService.saveFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("Uploading image to file: {}: {}", file.isEmpty(), file.getOriginalFilename());
        return ResponseEntity.ok(imageHolder.getOriginalFileName());
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        // 1. Load the file asset via the service layer
        Resource resource = fileService.loadFileAsResource(fileName);

        // 2. Dynamically determine the file's content/media type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // Fallback to a generic binary stream if type cannot be detected
            contentType = "application/octet-stream";
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                // USE THIS FOR SAVING DIRECTLY TO DISK:
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
