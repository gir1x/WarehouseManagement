package com.example.wms.service;

import com.example.wms.domain.ItemStock;
import com.example.wms.domain.Product;
import com.example.wms.domain.SlotStatus;
import com.example.wms.domain.Warehouse;
import com.example.wms.dto.DashboardSummaryDto;
import com.example.wms.dto.ProductStockDto;
import com.example.wms.dto.StockMovementDto;
import com.example.wms.dto.WarehouseSummaryDto;
import com.example.wms.repository.ItemStockRepository;
import com.example.wms.repository.ProductRepository;
import com.example.wms.repository.StockMovementRepository;
import com.example.wms.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rolls up everything the dashboard overview needs — occupancy across every
 * warehouse, which warehouses are approaching full capacity, which products
 * are running low system-wide, and the most recent receive/pick activity —
 * into a single read.
 */
@Service
public class DashboardService {

    /** A warehouse at or above this utilization shows up as "near full". */
    private static final double NEAR_FULL_THRESHOLD_PERCENT = 85.0;

    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final ItemStockRepository itemStockRepository;
    private final StockMovementRepository movementRepository;

    public DashboardService(WarehouseRepository warehouseRepository,
                             ProductRepository productRepository,
                             ItemStockRepository itemStockRepository,
                             StockMovementRepository movementRepository) {
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.itemStockRepository = itemStockRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummary() {
        List<WarehouseSummaryDto> warehouseSummaries = warehouseRepository.findAll().stream()
                .map(this::summarize)
                .toList();

        long totalSlots = warehouseSummaries.stream().mapToLong(WarehouseSummaryDto::totalSlots).sum();
        long filledSlots = warehouseSummaries.stream().mapToLong(WarehouseSummaryDto::filledSlots).sum();
        long availableSlots = totalSlots - filledSlots;
        double utilization = totalSlots == 0 ? 0.0 : (filledSlots * 100.0) / totalSlots;

        List<ProductStockDto> lowStock = lowStockProducts();

        List<StockMovementDto> recentActivity = movementRepository.findTop20ByOrderByOccurredAtDesc().stream()
                .map(StockMovementDto::from)
                .toList();

        return new DashboardSummaryDto(
                warehouseSummaries.size(),
                productRepository.count(),
                totalSlots,
                filledSlots,
                availableSlots,
                utilization,
                warehouseSummaries,
                lowStock,
                recentActivity
        );
    }

    private WarehouseSummaryDto summarize(Warehouse warehouse) {
        long total = warehouse.getSlots().size();
        long filled = warehouse.getSlots().stream().filter(s -> s.getStatus() == SlotStatus.OCCUPIED).count();
        long available = total - filled;
        double pct = total == 0 ? 0.0 : (filled * 100.0) / total;
        return new WarehouseSummaryDto(warehouse.getId(), warehouse.getName(), total, filled, available, pct,
                pct >= NEAR_FULL_THRESHOLD_PERCENT);
    }

    /** Every product whose total on-hand quantity (summed across all warehouses) is at or below its reorder threshold. */
    private List<ProductStockDto> lowStockProducts() {
        Map<UUID, Integer> onHandByProductId = new HashMap<>();
        for (ItemStock stock : itemStockRepository.findAll()) {
            onHandByProductId.merge(stock.getProduct().getId(), stock.getQuantity(), Integer::sum);
        }

        return productRepository.findAll().stream()
                .map(product -> toStockDto(product, onHandByProductId.getOrDefault(product.getId(), 0)))
                .filter(ProductStockDto::lowStock)
                .sorted(Comparator.comparingInt(ProductStockDto::totalOnHand))
                .toList();
    }

    private ProductStockDto toStockDto(Product product, int totalOnHand) {
        boolean low = totalOnHand <= product.getReorderThreshold();
        return new ProductStockDto(product.getSku(), product.getName(), totalOnHand, product.getUnit(), product.getReorderThreshold(), low);
    }
}
