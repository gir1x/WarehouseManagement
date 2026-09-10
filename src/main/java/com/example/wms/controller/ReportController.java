package com.example.wms.controller;

import com.example.wms.dto.OccupancyReportDto;
import com.example.wms.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Read-only: how many slots are filled vs. available right now (FR-7). */
@RestController
@RequestMapping("/api/warehouses/{warehouseId}/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/occupancy")
    public ResponseEntity<OccupancyReportDto> occupancy(@PathVariable UUID warehouseId) {
        return ResponseEntity.ok(reportService.getOccupancyReport(warehouseId));
    }
}
