package com.example.wms.controller;

import com.example.wms.domain.Slot;
import com.example.wms.domain.Warehouse;
import com.example.wms.dto.SlotDto;
import com.example.wms.dto.StockMovementDto;
import com.example.wms.dto.WarehouseRequest;
import com.example.wms.dto.WarehouseResponse;
import com.example.wms.repository.StockMovementRepository;
import com.example.wms.service.WarehouseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * @RestController tells Spring "every method here returns data (JSON), not
 * an HTML page." @RequestMapping sets the shared URL prefix for everything
 * below. This controller covers warehouse setup, the floor-plan grid view,
 * and the per-warehouse movement history (audit log).
 */
@RestController
@RequestMapping("/api/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final StockMovementRepository movementRepository;

    public WarehouseController(WarehouseService warehouseService, StockMovementRepository movementRepository) {
        this.warehouseService = warehouseService;
        this.movementRepository = movementRepository;
    }

    /** ADMIN only — enforced both at the URL level (SecurityConfig) and the service level (@PreAuthorize). */
    @PostMapping
    public ResponseEntity<WarehouseResponse> create(@Valid @RequestBody WarehouseRequest request) {
        Warehouse warehouse = warehouseService.createWarehouse(request.name(), request.aisles(), request.tiers());
        return ResponseEntity.status(HttpStatus.CREATED).body(WarehouseResponse.from(warehouse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(WarehouseResponse.from(warehouseService.getWarehouse(id)));
    }

    /** ADMIN + VISITOR — used by the /warehouses list page to populate the picker. */
    @GetMapping
    public ResponseEntity<List<WarehouseResponse>> list() {
        List<WarehouseResponse> warehouses = warehouseService.listWarehouses().stream()
                .map(WarehouseResponse::from)
                .toList();
        return ResponseEntity.ok(warehouses);
    }

    /** ADMIN + VISITOR — "view what's in which aisle/tier" (FR-5). */
    @GetMapping("/{id}/grid")
    public ResponseEntity<List<SlotDto>> grid(@PathVariable UUID id) {
        Warehouse warehouse = warehouseService.getWarehouse(id);
        List<SlotDto> slots = warehouse.getSlots().stream()
                .sorted(Comparator.comparingInt(Slot::getAisle).thenComparingInt(Slot::getTier))
                .map(SlotDto::from)
                .toList();
        return ResponseEntity.ok(slots);
    }

    /** ADMIN + VISITOR — audit log of every receive/pick against this warehouse, most recent first. */
    @GetMapping("/{id}/movements")
    public ResponseEntity<List<StockMovementDto>> movements(@PathVariable UUID id) {
        List<StockMovementDto> movements = movementRepository.findAllBySlot_Warehouse_IdOrderByOccurredAtDesc(id).stream()
                .map(StockMovementDto::from)
                .toList();
        return ResponseEntity.ok(movements);
    }
}
