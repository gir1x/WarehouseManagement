package com.example.wms.controller;

import com.example.wms.domain.Product;
import com.example.wms.dto.ProductRequest;
import com.example.wms.dto.ProductResponse;
import com.example.wms.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * The product catalog — the list of SKUs that can be received into any
 * warehouse. Creating/deleting is ADMIN-only; anyone with access can view the list.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /** ADMIN only — enforced at the URL level (SecurityConfig) and again in ProductService (@PreAuthorize). */
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        Product product = productService.createProduct(
                request.sku(), request.name(), request.reorderThreshold(), request.unit());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(product));
    }

    /** ADMIN + VISITOR — used by the products page and the receive form's SKU picker. */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> list() {
        List<ProductResponse> products = productService.listProducts().stream()
                .map(ProductResponse::from)
                .toList();
        return ResponseEntity.ok(products);
    }

    /** ADMIN only — refused if the product is currently stocked in any warehouse (see ProductService). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
