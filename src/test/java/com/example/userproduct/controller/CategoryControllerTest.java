package com.example.userproduct.controller;

import com.example.userproduct.security.CustomUserDetailsService;
import com.example.userproduct.security.JwtUtil;
import com.example.userproduct.security.SecurityConfig;
import com.example.userproduct.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class) @AutoConfigureMockMvc @Import(SecurityConfig.class)
class CategoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ProductService productService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    private UsernamePasswordAuthenticationToken authAs(String role) {
        return new UsernamePasswordAuthenticationToken("user@example.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    @Test @DisplayName("GET /api/categories returns 200 with list")
    void getAllCategories_returns200() throws Exception {
        when(productService.getAllCategories()).thenReturn(List.of("Pizza", "Burgers", "Sushi"));
        mockMvc.perform(get("/api/categories").with(authentication(authAs("CUSTOMER"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("Pizza"));
    }

    @Test @DisplayName("GET /api/categories without auth returns 401")
    void getAllCategories_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/categories")).andExpect(status().isUnauthorized());
    }
}
