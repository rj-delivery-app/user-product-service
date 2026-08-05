package com.example.userproduct.service;

import com.example.userproduct.dao.ProductRepository;
import com.example.userproduct.dao.UserRepository;
import com.example.userproduct.dto.CreateProductRequest;
import com.example.userproduct.dto.ProductResponse;
import com.example.userproduct.dto.UpdateProductRequest;
import com.example.userproduct.entities.Product;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProductService productService;

    private User merchant;
    private User customer;
    private Product product;
    private CreateProductRequest createRequest;
    private UpdateProductRequest updateRequest;

    @BeforeEach
    void setUp() {
        merchant = User.builder()
                .id("merchant-1")
                .name("Merchant One")
                .email("merchant@test.com")
                .role(UserRole.MERCHANT_ADMIN)
                .build();

        customer = User.builder()
                .id("customer-1")
                .name("Customer")
                .email("customer@test.com")
                .role(UserRole.CUSTOMER)
                .build();

        product = Product.builder()
                .id("product-1")
                .merchant(merchant)
                .name("Pizza")
                .description("Delicious pizza")
                .price(new BigDecimal("12.99"))
                .imageUrl("http://img.com/pizza.jpg")
                .category("Italian")
                .isAvailable(true)
                .build();

        createRequest = CreateProductRequest.builder()
                .name("Pizza")
                .description("Delicious pizza")
                .price(new BigDecimal("12.99"))
                .imageUrl("http://img.com/pizza.jpg")
                .category("Italian")
                .build();

        updateRequest = UpdateProductRequest.builder()
                .name("Updated Pizza")
                .description("Even more delicious")
                .price(new BigDecimal("14.99"))
                .imageUrl("http://img.com/pizza2.jpg")
                .category("Italian")
                .build();
    }

    @Nested
    @DisplayName("createProduct()")
    class CreateProduct {

        @Test
        @DisplayName("Merchant creates product successfully")
        void createProduct_merchantCreates_success() {
            when(userRepository.findById("merchant-1")).thenReturn(Optional.of(merchant));
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
                Product p = inv.getArgument(0);
                p.setId("product-new");
                return p;
            });

            ProductResponse response = productService.createProduct("merchant-1", createRequest);

            assertThat(response.getName()).isEqualTo("Pizza");
            assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("12.99"));
            assertThat(response.getIsAvailable()).isTrue();
            assertThat(response.getMerchantId()).isEqualTo("merchant-1");
        }

        @Test
        @DisplayName("Non-merchant throws")
        void createProduct_nonMerchant_throws() {
            when(userRepository.findById("customer-1")).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> productService.createProduct("customer-1", createRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Only merchants can create products");
        }
    }

    @Nested
    @DisplayName("updateProduct()")
    class UpdateProduct {

        @Test
        @DisplayName("Owner updates successfully")
        void updateProduct_ownerUpdates_success() {
            when(productRepository.findById("product-1")).thenReturn(Optional.of(product));
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

            ProductResponse response = productService.updateProduct("product-1", "merchant-1", updateRequest);

            assertThat(response.getName()).isEqualTo("Updated Pizza");
            assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("14.99"));
        }

        @Test
        @DisplayName("Wrong owner throws")
        void updateProduct_wrongOwner_throws() {
            when(productRepository.findById("product-1")).thenReturn(Optional.of(product));

            assertThatThrownBy(() -> productService.updateProduct("product-1", "merchant-2", updateRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("You can only manage your own products");
        }

        @Test
        @DisplayName("Not found throws")
        void updateProduct_notFound_throws() {
            when(productRepository.findById("product-1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateProduct("product-1", "merchant-1", updateRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Product not found");
        }
    }

    @Nested
    @DisplayName("deleteProduct()")
    class DeleteProduct {

        @Test
        @DisplayName("Owner deletes successfully")
        void deleteProduct_ownerDeletes_success() {
            when(productRepository.findById("product-1")).thenReturn(Optional.of(product));

            productService.deleteProduct("product-1", "merchant-1");

            verify(productRepository).delete(product);
        }

        @Test
        @DisplayName("Wrong owner throws")
        void deleteProduct_wrongOwner_throws() {
            when(productRepository.findById("product-1")).thenReturn(Optional.of(product));

            assertThatThrownBy(() -> productService.deleteProduct("product-1", "merchant-2"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("You can only manage your own products");
        }

        @Test
        @DisplayName("Not found throws")
        void deleteProduct_notFound_throws() {
            when(productRepository.findById("product-1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.deleteProduct("product-1", "merchant-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Product not found");
        }
    }

    @Nested
    @DisplayName("getProduct()")
    class GetProduct {

        @Test
        @DisplayName("Found returns response")
        void getProduct_found_returnsResponse() {
            when(productRepository.findById("product-1")).thenReturn(Optional.of(product));

            ProductResponse response = productService.getProduct("product-1");

            assertThat(response.getId()).isEqualTo("product-1");
            assertThat(response.getName()).isEqualTo("Pizza");
        }

        @Test
        @DisplayName("Not found throws")
        void getProduct_notFound_throws() {
            when(productRepository.findById("product-1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProduct("product-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Product not found");
        }
    }

    @Nested
    @DisplayName("getAllProducts()")
    class GetAllProducts {

        @Test
        @DisplayName("Returns paginated available products")
        void getAllProducts_returnsPaginated() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);
            when(productRepository.findByIsAvailableTrue(pageable)).thenReturn(page);

            Page<ProductResponse> result = productService.getAllProducts(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Pizza");
        }
    }

    @Nested
    @DisplayName("getProductsByMerchant()")
    class GetProductsByMerchant {

        @Test
        @DisplayName("Returns merchant's products")
        void getProductsByMerchant_returnsMerchantsProducts() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);
            when(productRepository.findByMerchantId("merchant-1", pageable)).thenReturn(page);

            Page<ProductResponse> result = productService.getProductsByMerchant("merchant-1", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getMerchantId()).isEqualTo("merchant-1");
        }
    }

    @Nested
    @DisplayName("getProductsByCategory()")
    class GetProductsByCategory {

        @Test
        @DisplayName("Filters by category")
        void getProductsByCategory_filtersByCategory() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);
            when(productRepository.findByCategory("Italian", pageable)).thenReturn(page);

            Page<ProductResponse> result = productService.getProductsByCategory("Italian", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getCategory()).isEqualTo("Italian");
        }
    }

    @Nested
    @DisplayName("toggleAvailability()")
    class ToggleAvailability {

        @Test
        @DisplayName("Flips availability flag")
        void toggleAvailability_flipsFlag() {
            when(productRepository.findById("product-1")).thenReturn(Optional.of(product));
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

            ProductResponse response = productService.toggleAvailability("product-1", "merchant-1");

            assertThat(response.getIsAvailable()).isFalse();
        }

        @Test
        @DisplayName("Wrong owner throws")
        void toggleAvailability_wrongOwner_throws() {
            when(productRepository.findById("product-1")).thenReturn(Optional.of(product));

            assertThatThrownBy(() -> productService.toggleAvailability("product-1", "merchant-2"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("You can only manage your own products");
        }

        @Test
        @DisplayName("Not found throws")
        void toggleAvailability_notFound_throws() {
            when(productRepository.findById("product-1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.toggleAvailability("product-1", "merchant-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Product not found");
        }
    }

    @Nested
    @DisplayName("searchProducts()")
    class SearchProducts {

        @Test
        @DisplayName("Delegates to repository")
        void searchProducts_delegatesToRepo() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);
            when(productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    "Pizza", "Pizza", pageable)).thenReturn(page);

            Page<ProductResponse> result = productService.searchProducts("Pizza", pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getAllCategories()")
    class GetAllCategories {

        @Test
        @DisplayName("Returns distinct list")
        void getAllCategories_returnsDistinctList() {
            when(productRepository.findDistinctCategories()).thenReturn(List.of("Italian", "Mexican"));

            List<String> categories = productService.getAllCategories();

            assertThat(categories).containsExactly("Italian", "Mexican");
        }
    }
}
