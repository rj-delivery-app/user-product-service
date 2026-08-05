package com.example.userproduct.controller;

import com.example.userproduct.dto.AddressRequest;
import com.example.userproduct.dto.AddressResponse;
import com.example.userproduct.entities.AddressType;
import com.example.userproduct.security.CustomUserDetailsService;
import com.example.userproduct.security.JwtUtil;
import com.example.userproduct.security.SecurityConfig;
import com.example.userproduct.service.AddressService;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AddressController.class) @AutoConfigureMockMvc @Import(SecurityConfig.class)
class AddressControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private AddressService addressService;
    @MockBean private UserContextService userContextService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    private UsernamePasswordAuthenticationToken authAs(String role) {
        return new UsernamePasswordAuthenticationToken("user@example.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    private AddressResponse buildSampleAddress() {
        return AddressResponse.builder().id("addr-1").addressType("HOME").street("123 Main St")
                .city("New York").state("NY").zipCode("10001").build();
    }

    private AddressRequest buildValidRequest() {
        return AddressRequest.builder().addressType(AddressType.HOME).street("123 Main St")
                .city("New York").state("NY").zipCode("10001").build();
    }

    @Nested @DisplayName("GET /api/users/addresses") class GetAddressesTests {
        @Test @DisplayName("Authenticated user gets addresses - 200")
        void getAddresses_authenticated_returns200() throws Exception {
            when(userContextService.getCurrentUserId()).thenReturn("user-1");
            when(addressService.getUserAddresses("user-1")).thenReturn(List.of(buildSampleAddress()));
            mockMvc.perform(get("/api/users/addresses").with(authentication(authAs("CUSTOMER"))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("addr-1"));
        }
        @Test @DisplayName("No auth returns 401")
        void getAddresses_noAuth_returns401() throws Exception {
            mockMvc.perform(get("/api/users/addresses")).andExpect(status().isUnauthorized());
        }
    }

    @Nested @DisplayName("GET /api/users/addresses/{id}") class GetAddressByIdTests {
        @Test @DisplayName("Existing address returns 200")
        void getAddress_found_returns200() throws Exception {
            when(userContextService.getCurrentUserId()).thenReturn("user-1");
            when(addressService.getAddress("addr-1", "user-1")).thenReturn(buildSampleAddress());
            mockMvc.perform(get("/api/users/addresses/{id}", "addr-1").with(authentication(authAs("CUSTOMER"))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("addr-1"));
        }
    }

    @Nested @DisplayName("POST /api/users/addresses") class CreateAddressTests {
        @Test @DisplayName("Valid address creation returns 201")
        void createAddress_valid_returns201() throws Exception {
            when(userContextService.getCurrentUserId()).thenReturn("user-1");
            when(addressService.createAddress(eq("user-1"), any(AddressRequest.class))).thenReturn(buildSampleAddress());
            mockMvc.perform(post("/api/users/addresses").with(authentication(authAs("CUSTOMER")))
                            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value("addr-1"));
        }
        @Test @DisplayName("Invalid address returns 400")
        void createAddress_invalid_returns400() throws Exception {
            AddressRequest request = AddressRequest.builder().addressType(null).street("").city("").state("").zipCode("").build();
            mockMvc.perform(post("/api/users/addresses").with(authentication(authAs("CUSTOMER")))
                            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
        @Test @DisplayName("No auth returns 401")
        void createAddress_noAuth_returns401() throws Exception {
            mockMvc.perform(post("/api/users/addresses").contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested @DisplayName("PUT /api/users/addresses/{id}") class UpdateAddressTests {
        @Test @DisplayName("Valid update returns 200")
        void updateAddress_valid_returns200() throws Exception {
            AddressRequest request = buildValidRequest(); request.setStreet("456 Updated Ave");
            AddressResponse response = buildSampleAddress(); response.setStreet("456 Updated Ave");
            when(userContextService.getCurrentUserId()).thenReturn("user-1");
            when(addressService.updateAddress(eq("addr-1"), eq("user-1"), any(AddressRequest.class))).thenReturn(response);
            mockMvc.perform(put("/api/users/addresses/{id}", "addr-1").with(authentication(authAs("CUSTOMER")))
                            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.street").value("456 Updated Ave"));
        }
    }

    @Nested @DisplayName("DELETE /api/users/addresses/{id}") class DeleteAddressTests {
        @Test @DisplayName("Delete address returns 204")
        void deleteAddress_returns204() throws Exception {
            when(userContextService.getCurrentUserId()).thenReturn("user-1");
            doNothing().when(addressService).deleteAddress("addr-1", "user-1");
            mockMvc.perform(delete("/api/users/addresses/{id}", "addr-1").with(authentication(authAs("CUSTOMER"))))
                    .andExpect(status().isNoContent());
        }
        @Test @DisplayName("No auth returns 401")
        void deleteAddress_noAuth_returns401() throws Exception {
            mockMvc.perform(delete("/api/users/addresses/{id}", "addr-1")).andExpect(status().isUnauthorized());
        }
    }
}
