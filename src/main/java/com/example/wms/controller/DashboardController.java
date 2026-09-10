package com.example.wms.controller;

import com.example.wms.dto.DashboardSummaryDto;
import com.example.wms.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** One call for the dashboard page: occupancy KPIs, near-full/low-stock alerts, and recent activity. */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> summary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }
}
