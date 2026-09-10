package com.example.wms.repository;

import com.example.wms.domain.ItemStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ItemStockRepository extends JpaRepository<ItemStock, UUID> {
    // Used by ProductService.deleteProduct() to refuse deleting a product
    // that's currently stocked somewhere, rather than orphaning/cascading it away.
    boolean existsByProduct_Id(UUID productId);
}
