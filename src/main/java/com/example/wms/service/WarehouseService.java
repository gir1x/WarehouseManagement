package com.example.wms.service;

import com.example.wms.domain.Warehouse;
import com.example.wms.exception.WarehouseNotFoundException;
import com.example.wms.repository.WarehouseRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    // Second, independent layer of access control below the URL rules in
    // SecurityConfig — see documentation §7.5 "Defense in Depth".
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Warehouse createWarehouse(String name, int aisles, int tiers) {
        Warehouse warehouse = new Warehouse.Builder()
                .name(name)
                .aisles(aisles)
                .tiers(tiers)
                .build();
        return warehouseRepository.save(warehouse);
    }

    @Transactional(readOnly = true)
    public Warehouse getWarehouse(UUID id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new WarehouseNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Warehouse> listWarehouses() {
        return warehouseRepository.findAll();
    }
}
