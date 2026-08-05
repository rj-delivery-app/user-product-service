package com.example.userproduct.service;

import com.example.userproduct.dao.AddressRepository;
import com.example.userproduct.dao.UserRepository;
import com.example.userproduct.dto.AddressRequest;
import com.example.userproduct.dto.AddressResponse;
import com.example.userproduct.entities.Address;
import com.example.userproduct.entities.AddressType;
import com.example.userproduct.entities.User;
import com.example.userproduct.entities.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddressService Tests")
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AddressService addressService;

    private User user;
    private Address address;
    private AddressRequest addressRequest;
    private final String userId = "user-1";
    private final String addressId = "addr-1";

    @BeforeEach
    void setUp() {
        user = User.builder().id(userId).name("John").email("john@test.com").role(UserRole.CUSTOMER).build();
        address = new Address();
        address.setId(addressId);
        address.setAddressType(AddressType.HOME);
        address.setStreet("123 Main St");
        address.setCity("NYC");
        address.setState("NY");
        address.setZipCode("10001");
        address.setUser(user);
        addressRequest = AddressRequest.builder()
                .addressType(AddressType.WORK).street("456 Office Ave").city("NYC").state("NY").zipCode("10002").build();
    }

    @Nested @DisplayName("getUserAddresses()") class GetUserAddresses {
        @Test @DisplayName("Returns user's addresses")
        void getUserAddresses_returnsAddresses() {
            when(addressRepository.findByUserId(userId)).thenReturn(List.of(address));
            List<AddressResponse> result = addressService.getUserAddresses(userId);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStreet()).isEqualTo("123 Main St");
        }
    }

    @Nested @DisplayName("getAddress()") class GetAddress {
        @Test @DisplayName("Found returns response")
        void getAddress_found_returnsResponse() {
            when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(address));
            AddressResponse response = addressService.getAddress(addressId, userId);
            assertThat(response.getId()).isEqualTo(addressId);
        }
        @Test @DisplayName("Not found throws")
        void getAddress_notFound_throws() {
            when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> addressService.getAddress(addressId, userId))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage("Address not found");
        }
    }

    @Nested @DisplayName("createAddress()") class CreateAddress {
        @Test @DisplayName("User found - saves address")
        void createAddress_userFound_saves() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(addressRepository.save(any(Address.class))).thenAnswer(inv -> { Address a = inv.getArgument(0); a.setId("new-addr"); return a; });
            AddressResponse response = addressService.createAddress(userId, addressRequest);
            assertThat(response.getStreet()).isEqualTo("456 Office Ave");
            verify(addressRepository).save(any(Address.class));
        }
        @Test @DisplayName("User not found throws")
        void createAddress_userNotFound_throws() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> addressService.createAddress(userId, addressRequest))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage("User not found");
        }
    }

    @Nested @DisplayName("updateAddress()") class UpdateAddress {
        @Test @DisplayName("Valid update succeeds")
        void updateAddress_validUpdate_succeeds() {
            when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(address));
            when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));
            AddressResponse response = addressService.updateAddress(addressId, userId, addressRequest);
            assertThat(response.getStreet()).isEqualTo("456 Office Ave");
        }
        @Test @DisplayName("Not found throws")
        void updateAddress_notFound_throws() {
            when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> addressService.updateAddress(addressId, userId, addressRequest))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage("Address not found");
        }
    }

    @Nested @DisplayName("deleteAddress()") class DeleteAddress {
        @Test @DisplayName("Valid delete succeeds")
        void deleteAddress_valid_succeeds() {
            when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(address));
            addressService.deleteAddress(addressId, userId);
            verify(addressRepository).delete(address);
        }
        @Test @DisplayName("Not found throws")
        void deleteAddress_notFound_throws() {
            when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> addressService.deleteAddress(addressId, userId))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage("Address not found");
        }
    }
}
