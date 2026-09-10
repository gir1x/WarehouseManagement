package com.example.wms.service;

import com.example.wms.domain.Slot;
import com.example.wms.domain.SlotStatus;
import com.example.wms.dto.OccupancyReportDto;
import com.example.wms.repository.SlotReader;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ReportService {

    private final SlotReader slotReader;

    public ReportService(SlotReader slotReader) {
        this.slotReader = slotReader;
    }

    public OccupancyReportDto getOccupancyReport(UUID warehouseId) {
        List<Slot> slots = slotReader.findAllByWarehouse_Id(warehouseId);

        long total = slots.size();
        long filled = slots.stream().filter(slot -> slot.getStatus() == SlotStatus.OCCUPIED).count();
        long available = total - filled;
        double utilization = total == 0 ? 0.0 : (filled * 100.0) / total;

        return new OccupancyReportDto(warehouseId, total, filled, available, utilization);
    }
}
