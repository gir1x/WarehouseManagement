package com.example.wms.service;

import com.example.wms.domain.Product;
import com.example.wms.exception.ProductNotFoundException;
import com.example.wms.exception.ValidationException;
import com.example.wms.repository.ItemStockRepository;
import com.example.wms.repository.ProductRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ItemStockRepository itemStockRepository = mock(ItemStockRepository.class);
    private final ProductService productService = new ProductService(productRepository, itemStockRepository);

    @Test
    void createsANewProduct() {
        when(productRepository.findBySku("SKU-002")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product created = productService.createProduct("SKU-002", "Pallet Wrap");

        assertThat(created.getSku()).isEqualTo("SKU-002");
        assertThat(created.getName()).isEqualTo("Pallet Wrap");
        assertThat(created.getUnit()).isEqualTo("pcs"); // default when not specified
    }

    @Test
    void createsAProductWithACustomUnit() {
        when(productRepository.findBySku("SKU-003")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product created = productService.createProduct("SKU-003", "Cooking Oil", 10, "litre");

        assertThat(created.getUnit()).isEqualTo("litre");
        assertThat(created.getReorderThreshold()).isEqualTo(10);
    }

    @Test
    void rejectsADuplicateSku() {
        when(productRepository.findBySku("SKU-001"))
                .thenReturn(Optional.of(new Product("SKU-001", "Sample Product")));

        assertThatThrownBy(() -> productService.createProduct("SKU-001", "Something else"))
                .isInstanceOf(ValidationException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void deletesAProductThatIsNotStockedAnywhere() {
        UUID id = UUID.randomUUID();
        Product product = new Product("SKU-004", "Unused Product");
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(itemStockRepository.existsByProduct_Id(id)).thenReturn(false);

        productService.deleteProduct(id);

        verify(productRepository).delete(product);
    }

    @Test
    void refusesToDeleteAProductThatIsCurrentlyStocked() {
        UUID id = UUID.randomUUID();
        Product product = new Product("SKU-005", "In-Use Product");
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(itemStockRepository.existsByProduct_Id(id)).thenReturn(true);

        assertThatThrownBy(() -> productService.deleteProduct(id))
                .isInstanceOf(ValidationException.class);

        verify(productRepository, never()).delete(any());
    }

    @Test
    void throwsWhenDeletingAnUnknownProduct() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(id))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
