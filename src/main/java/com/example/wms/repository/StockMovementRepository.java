package com.example.wms.repository;

import com.example.wms.domain.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {
    List<StockMovement> findAllBySlot_Warehouse_IdOrderByOccurredAtDesc(UUID warehouseId);

    List<StockMovement> findTop20ByOrderByOccurredAtDesc();
}
