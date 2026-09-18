package com.example.userproduct.controller;

import com.example.userproduct.dao.ProductRepository;
import com.example.userproduct.dao.UserRepository;
import com.example.userproduct.dto.CreateProductRequest;
import com.example.userproduct.dto.ImageHolder;
import com.example.userproduct.dto.ProductResponse;
import com.example.userproduct.dto.UpdateProductRequest;
import com.example.userproduct.security.CustomUserDetailsService;
import com.example.userproduct.security.JwtUtil;
import com.example.userproduct.security.SecurityConfig;
import com.example.userproduct.service.FileService;
import com.example.userproduct.service.ProductService;
import com.example.userproduct.service.UserContextService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class) @AutoConfigureMockMvc @Import(SecurityConfig.class)
class ProductControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ProductService productService;
    @MockBean private UserContextService userContextService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private ImageHolder imageHolder;
    @MockBean private FileService fileService;

    private UsernamePasswordAuthenticationToken authAs(String role) {
        return new UsernamePasswordAuthenticationToken("user@example.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    private ProductResponse buildSampleProduct() {
        return ProductResponse.builder().id("prod-1").merchantId("merchant-1").merchantName("Pizza Palace")
                .name("Margherita Pizza").description("Classic pizza").price(new BigDecimal("12.99"))
                .category("Pizza").isAvailable(true).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    @Nested @DisplayName("POST /api/products") class CreateProductTests {
        @Test @DisplayName("MERCHANT_ADMIN creates product - 201")
        void createProduct_merchantAdmin_returns201() throws Exception {
            MockHttpSession session = new MockHttpSession();
            CreateProductRequest request = CreateProductRequest.builder().name("Margherita Pizza")
                    .description("Classic pizza").price(new BigDecimal("12.99")).category("Pizza").build();
            when(userContextService.getCurrentUserId()).thenReturn("merchant-1");
            when(productService.createProduct(eq("merchant-1"), any(CreateProductRequest.class))).thenReturn(buildSampleProduct());
            mockMvc.perform(post("/api/products").with(authentication(authAs("MERCHANT_ADMIN")))
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value("prod-1"));
        }
        @Test @DisplayName("CUSTOMER role gets 403")
        void createProduct_customer_returns403() throws Exception {
            CreateProductRequest request = CreateProductRequest.builder().name("Pizza").description("Test")
                    .price(new BigDecimal("12.99")).category("Pizza").build();
            mockMvc.perform(post("/api/products").with(authentication(authAs("CUSTOMER")))
                            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
        @Test @DisplayName("Missing required fields returns 400")
        void createProduct_invalidBody_returns400() throws Exception {
            CreateProductRequest request = CreateProductRequest.builder().name("").price(null).category("").build();
            mockMvc.perform(post("/api/products").with(authentication(authAs("MERCHANT_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested @DisplayName("PUT /api/products/{id}") class UpdateProductTests {
        @Test @DisplayName("Valid update returns 200")
        void updateProduct_valid_returns200() throws Exception {
            UpdateProductRequest request = UpdateProductRequest.builder().name("Updated Pizza")
                    .description("Updated").price(new BigDecimal("14.99")).category("Pizza").build();
            ProductResponse response = buildSampleProduct(); response.setName("Updated Pizza");
            when(userContextService.getCurrentUserId()).thenReturn("merchant-1");
            when(productService.updateProduct(eq("prod-1"), eq("merchant-1"), any(UpdateProductRequest.class))).thenReturn(response);
            mockMvc.perform(put("/api/products/{id}", "prod-1").with(authentication(authAs("MERCHANT_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated Pizza"));
        }
        @Test @DisplayName("CUSTOMER role gets 403")
        void updateProduct_customer_returns403() throws Exception {
            UpdateProductRequest request = UpdateProductRequest.builder().name("Updated").description("Desc")
                    .price(new BigDecimal("14.99")).category("Pizza").build();
            mockMvc.perform(put("/api/products/{id}", "prod-1").with(authentication(authAs("CUSTOMER")))
                            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested @DisplayName("DELETE /api/products/{id}") class DeleteProductTests {
        @Test @DisplayName("MERCHANT_ADMIN deletes product - 204")
        void deleteProduct_merchantAdmin_returns204() throws Exception {
            when(userContextService.getCurrentUserId()).thenReturn("merchant-1");
            doNothing().when(productService).deleteProduct("prod-1", "merchant-1");
            mockMvc.perform(delete("/api/products/{id}", "prod-1").with(authentication(authAs("MERCHANT_ADMIN"))))
                    .andExpect(status().isNoContent());
        }
        @Test @DisplayName("CUSTOMER role gets 403")
        void deleteProduct_customer_returns403() throws Exception {
            mockMvc.perform(delete("/api/products/{id}", "prod-1").with(authentication(authAs("CUSTOMER"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested @DisplayName("GET /api/products/{id}") class GetProductTests {
        @Test @DisplayName("Existing product returns 200")
        void getProduct_found_returns200() throws Exception {
            when(productService.getProduct("prod-1")).thenReturn(buildSampleProduct());
            mockMvc.perform(get("/api/products/{id}", "prod-1").with(authentication(authAs("CUSTOMER"))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("prod-1"));
        }
    }

    @Nested @DisplayName("GET /api/products") class GetAllProductsTests {
        @Test @DisplayName("Paginated products returns 200")
        void getAllProducts_returns200() throws Exception {
            Page<ProductResponse> page = new PageImpl<>(List.of(buildSampleProduct()));
            when(productService.getAllProducts(any(Pageable.class))).thenReturn(page);
            mockMvc.perform(get("/api/products").with(authentication(authAs("CUSTOMER"))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value("prod-1"));
        }
    }

    @Nested @DisplayName("GET /api/products/merchant/{merchantId}") class GetProductsByMerchantTests {
        @Test @DisplayName("Products by merchant returns 200")
        void getByMerchant_returns200() throws Exception {
            Page<ProductResponse> page = new PageImpl<>(List.of(buildSampleProduct()));
            when(productService.getProductsByMerchant(eq("merchant-1"), any(Pageable.class))).thenReturn(page);
            mockMvc.perform(get("/api/products/merchant/{merchantId}", "merchant-1").with(authentication(authAs("CUSTOMER"))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].merchantId").value("merchant-1"));
        }
    }

    @Nested @DisplayName("GET /api/products/category/{category}") class GetProductsByCategoryTests {
        @Test @DisplayName("Products by category returns 200")
        void getByCategory_returns200() throws Exception {
            Page<ProductResponse> page = new PageImpl<>(List.of(buildSampleProduct()));
            when(productService.getProductsByCategory(eq("Pizza"), any(Pageable.class))).thenReturn(page);
            mockMvc.perform(get("/api/products/category/{category}", "Pizza").with(authentication(authAs("CUSTOMER"))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].category").value("Pizza"));
        }
    }

    @Nested @DisplayName("PATCH /api/products/{id}/availability") class ToggleAvailabilityTests {
        @Test @DisplayName("MERCHANT_ADMIN toggles availability - 200")
        void toggleAvailability_merchantAdmin_returns200() throws Exception {
            ProductResponse response = buildSampleProduct(); response.setIsAvailable(false);
            when(userContextService.getCurrentUserId()).thenReturn("merchant-1");
            when(productService.toggleAvailability("prod-1", "merchant-1")).thenReturn(response);
            mockMvc.perform(patch("/api/products/{id}/availability", "prod-1").with(authentication(authAs("MERCHANT_ADMIN"))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.isAvailable").value(false));
        }
        @Test @DisplayName("CUSTOMER role gets 403")
        void toggleAvailability_customer_returns403() throws Exception {
            mockMvc.perform(patch("/api/products/{id}/availability", "prod-1").with(authentication(authAs("CUSTOMER"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested @DisplayName("GET /api/products/search") class SearchProductsTests {
        @Test @DisplayName("Search returns 200 with results")
        void searchProducts_returns200() throws Exception {
            Page<ProductResponse> page = new PageImpl<>(List.of(buildSampleProduct()));
            when(productService.searchProducts(eq("pizza"), any(Pageable.class))).thenReturn(page);
            mockMvc.perform(get("/api/products/search").param("query", "pizza").with(authentication(authAs("CUSTOMER"))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].name").value("Margherita Pizza"));
        }
    }
}
