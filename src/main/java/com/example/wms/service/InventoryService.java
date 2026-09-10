package com.example.wms.service;

import com.example.wms.domain.ItemStock;
import com.example.wms.domain.Product;
import com.example.wms.domain.Slot;
import com.example.wms.domain.SlotStatus;
import com.example.wms.domain.StockMovement;
import com.example.wms.dto.PickCommand;
import com.example.wms.dto.PickResult;
import com.example.wms.dto.ReceiveCommand;
import com.example.wms.dto.ReceiveResult;
import com.example.wms.exception.SlotNotFoundException;
import com.example.wms.exception.ValidationException;
import com.example.wms.repository.ItemStockRepository;
import com.example.wms.repository.ProductRepository;
import com.example.wms.repository.SlotRepository;
import com.example.wms.repository.StockMovementRepository;
import com.example.wms.service.allocation.SlotAllocationStrategy;
import com.example.wms.service.validation.ReceiveValidationHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FACADE PATTERN
 * --------------
 * Receiving/picking an item touches validation, allocation, the State
 * pattern on Slot, persistence, and audit logging. Controllers only ever
 * call receiveItem(...) / pickItem(...) here — everything underneath is
 * coordinated in one place instead of being spread across the controller.
 */
@Service
public class InventoryService {

    private final ReceiveValidationHandler validationChain;
    private final SlotAllocationStrategy allocationStrategy;
    private final SlotRepository slotRepository;
    private final ProductRepository productRepository;
    private final ItemStockRepository itemStockRepository;
    private final StockMovementRepository movementRepository;

    public InventoryService(ReceiveValidationHandler validationChain,
                             SlotAllocationStrategy allocationStrategy,
                             SlotRepository slotRepository,
                             ProductRepository productRepository,
                             ItemStockRepository itemStockRepository,
                             StockMovementRepository movementRepository) {
        this.validationChain = validationChain;
        this.allocationStrategy = allocationStrategy;
        this.slotRepository = slotRepository;
        this.productRepository = productRepository;
        this.itemStockRepository = itemStockRepository;
        this.movementRepository = movementRepository;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'VISITOR')")
    @Transactional
    public ReceiveResult receiveItem(ReceiveCommand command) {
        validationChain.validate(command);

        Product product = productRepository.findBySku(command.productSku())
                .orElseThrow(() -> new ValidationException("Unknown product SKU: " + command.productSku()));

        Slot slot = resolveDestinationSlot(command);

        ItemStock itemStock = new ItemStock(product, command.quantity());
        slot.receive(itemStock); // State pattern: EmptyState -> OccupiedState

        itemStockRepository.save(itemStock);
        Slot savedSlot = slotRepository.save(slot);

        movementRepository.save(
                StockMovement.receive(savedSlot, product.getSku(), command.quantity(), command.performedBy())
        );

        return new ReceiveResult(savedSlot.getId(), savedSlot.getAisle(), savedSlot.getTier());
    }

    /**
     * If the caller chose a specific empty slot on the floor plan
     * (command.preferredSlotId() != null), use it after validating it's
     * actually free and belongs to this warehouse. Otherwise fall back to
     * the Strategy pattern's automatic choice — this keeps existing callers
     * that never send a slotId working exactly as before.
     */
    private Slot resolveDestinationSlot(ReceiveCommand command) {
        if (command.preferredSlotId() == null) {
            return allocationStrategy.findSlot(command.warehouseId());
        }

        Slot slot = slotRepository.findById(command.preferredSlotId())
                .orElseThrow(() -> new SlotNotFoundException(command.preferredSlotId()));

        if (!slot.getWarehouse().getId().equals(command.warehouseId())) {
            throw new ValidationException("That slot doesn't belong to this warehouse");
        }
        if (slot.getStatus() != SlotStatus.EMPTY) {
            throw new ValidationException("That slot is already occupied — choose an empty one");
        }
        return slot;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'VISITOR')")
    @Transactional
    public PickResult pickItem(PickCommand command) {
        Slot slot = slotRepository.findById(command.slotId())
                .orElseThrow(() -> new SlotNotFoundException(command.slotId()));

        if (slot.getStatus() != SlotStatus.OCCUPIED || slot.getItemStock() == null) {
            throw new ValidationException("Slot is not occupied: " + command.slotId());
        }

        ItemStock itemStock = slot.getItemStock();
        String sku = itemStock.getProduct().getSku();

        // State pattern: partial pick reduces stock and stays OCCUPIED; picking
        // everything transitions OccupiedState -> EmptyState (and removes the
        // ItemStock row via orphanRemoval).
        slot.pick(command.quantity());
        Slot savedSlot = slotRepository.save(slot);

        movementRepository.save(
                StockMovement.pick(savedSlot, sku, command.quantity(), command.performedBy())
        );

        return new PickResult(savedSlot.getId(), savedSlot.getAisle(), savedSlot.getTier());
    }
}
