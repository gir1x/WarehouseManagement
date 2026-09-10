package com.example.wms.controller;

import com.example.wms.dto.PickRequest;
import com.example.wms.dto.PickResult;
import com.example.wms.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Handles "pick X units out of this slot" requests. Only the actual quantity
 * requested is removed — see InventoryService.pickItem() and the State
 * pattern (domain/OccupiedState.java) for how a PARTIAL pick works.
 */
@RestController
@RequestMapping("/api/warehouses/{warehouseId}/pick")
public class PickingController {

    private final InventoryService inventoryService;

    public PickingController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<PickResult> pick(@PathVariable UUID warehouseId,
                                            @Valid @RequestBody PickRequest request,
                                            Authentication authentication) {
        PickResult result = inventoryService.pickItem(request.toCommand(authentication.getName()));
        return ResponseEntity.ok(result);
    }
}
