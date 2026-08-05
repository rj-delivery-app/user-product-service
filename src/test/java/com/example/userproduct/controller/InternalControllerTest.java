package com.example.userproduct.controller;

import com.example.userproduct.dao.AddressRepository;
import com.example.userproduct.dao.ProductRepository;
import com.example.userproduct.dao.UserRepository;
import com.example.userproduct.entities.*;
import com.example.userproduct.security.CustomUserDetailsService;
import com.example.userproduct.security.JwtUtil;
import com.example.userproduct.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InternalController.class) @AutoConfigureMockMvc @Import(SecurityConfig.class)
@DisplayName("InternalController Tests")
class InternalControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private UserRepository userRepository;
    @MockBean private ProductRepository productRepository;
    @MockBean private AddressRepository addressRepository;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    @Nested @DisplayName("GET /internal/users/{id}") class GetUserByIdTests {
        @Test @DisplayName("Returns user without authentication - 200")
        void getUserById_noAuth_returns200() throws Exception {
            User user = User.builder().id("user-1").name("John Doe").email("john@example.com")
                    .role(UserRole.CUSTOMER).isActive(true).build();
            when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
            mockMvc.perform(get("/api/internal/users/{id}", "user-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("user-1"))
                    .andExpect(jsonPath("$.name").value("John Doe"))
                    .andExpect(jsonPath("$.role").value("CUSTOMER"))
                    .andExpect(jsonPath("$.isActive").value(true));
        }
    }

    @Nested @DisplayName("GET /internal/users/{userId}/addresses/{addressId}") class GetUserAddressTests {
        @Test @DisplayName("Returns address without authentication - 200")
        void getUserAddress_noAuth_returns200() throws Exception {
            User user = User.builder().id("user-1").name("John").build();
            Address address = new Address();
            address.setId("addr-1"); address.setStreet("123 Main St"); address.setCity("NYC");
            address.setState("NY"); address.setZipCode("10001"); address.setAddressType(AddressType.HOME);
            address.setUser(user);
            when(addressRepository.findByIdAndUserId("addr-1", "user-1")).thenReturn(Optional.of(address));
            mockMvc.perform(get("/api/internal/users/{userId}/addresses/{addressId}", "user-1", "addr-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("addr-1"))
                    .andExpect(jsonPath("$.street").value("123 Main St"))
                    .andExpect(jsonPath("$.type").value("HOME"));
        }
    }

    @Nested @DisplayName("GET /internal/products/{id}") class GetProductByIdTests {
        @Test @DisplayName("Returns product without authentication - 200")
        void getProductById_noAuth_returns200() throws Exception {
            User merchant = User.builder().id("merchant-1").name("Merchant").build();
            Product product = Product.builder().id("prod-1").merchant(merchant).name("Pizza")
                    .price(new BigDecimal("12.99")).isAvailable(true).build();
            when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
            mockMvc.perform(get("/api/internal/products/{id}", "prod-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("prod-1"))
                    .andExpect(jsonPath("$.merchantId").value("merchant-1"))
                    .andExpect(jsonPath("$.name").value("Pizza"));
        }
    }

    @Nested @DisplayName("GET /internal/products/batch") class GetProductsBatchTests {
        @Test @DisplayName("Returns batch products without authentication - 200")
        void getProductsByIds_noAuth_returns200() throws Exception {
            User merchant = User.builder().id("merchant-1").name("Merchant").build();
            Product product1 = Product.builder().id("prod-1").merchant(merchant).name("Pizza")
                    .price(new BigDecimal("12.99")).isAvailable(true).build();
            Product product2 = Product.builder().id("prod-2").merchant(merchant).name("Burger")
                    .price(new BigDecimal("9.99")).isAvailable(true).build();
            when(productRepository.findAllById(List.of("prod-1", "prod-2"))).thenReturn(List.of(product1, product2));
            mockMvc.perform(get("/api/internal/products/batch").param("ids", "prod-1", "prod-2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value("prod-1"))
                    .andExpect(jsonPath("$[1].id").value("prod-2"));
        }
    }
}
