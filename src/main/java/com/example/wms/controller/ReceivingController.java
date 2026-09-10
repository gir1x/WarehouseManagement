package com.example.wms.controller;

import com.example.wms.dto.ReceiveRequest;
import com.example.wms.dto.ReceiveResult;
import com.example.wms.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Handles "put this item into the warehouse" requests. If the caller sends a
 * slotId, that exact slot is used (as long as it's empty); otherwise the
 * Strategy pattern (service/allocation/) picks one automatically.
 */
@RestController
@RequestMapping("/api/warehouses/{warehouseId}/receive")
public class ReceivingController {

    private final InventoryService inventoryService;

    public ReceivingController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping
    public ResponseEntity<ReceiveResult> receive(@PathVariable UUID warehouseId,
                                                   @Valid @RequestBody ReceiveRequest request,
                                                   Authentication authentication) {
        ReceiveResult result = inventoryService.receiveItem(
                request.toCommand(warehouseId, authentication.getName())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
