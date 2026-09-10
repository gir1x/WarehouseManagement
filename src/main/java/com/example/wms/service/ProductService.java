package com.example.wms.service;

import com.example.wms.domain.Product;
import com.example.wms.exception.ProductNotFoundException;
import com.example.wms.exception.ValidationException;
import com.example.wms.repository.ItemStockRepository;
import com.example.wms.repository.ProductRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ItemStockRepository itemStockRepository;

    public ProductService(ProductRepository productRepository, ItemStockRepository itemStockRepository) {
        this.productRepository = productRepository;
        this.itemStockRepository = itemStockRepository;
    }

    /** ADMIN only — product catalog is master data, same tier as warehouse setup. */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Product createProduct(String sku, String name) {
        return createProduct(sku, name, null, null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Product createProduct(String sku, String name, Integer reorderThreshold) {
        return createProduct(sku, name, reorderThreshold, null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Product createProduct(String sku, String name, Integer reorderThreshold, String unit) {
        if (productRepository.findBySku(sku).isPresent()) {
            throw new ValidationException("A product with SKU '" + sku + "' already exists");
        }
        return productRepository.save(new Product(sku, name, reorderThreshold, unit));
    }

    @Transactional(readOnly = true)
    public List<Product> listProducts() {
        return productRepository.findAll();
    }

    /**
     * ADMIN only. Refuses to delete a product that's currently sitting in any
     * warehouse slot — deleting it would either orphan that ItemStock row or
     * cascade-delete it silently, either of which loses real inventory data
     * without the admin necessarily meaning to. Pick the stock out first,
     * then delete the product.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (itemStockRepository.existsByProduct_Id(id)) {
            throw new ValidationException(
                    "Can't delete '" + product.getSku() + "' — it's currently stocked in a warehouse. Pick it out first.");
        }

        productRepository.delete(product);
    }
}
