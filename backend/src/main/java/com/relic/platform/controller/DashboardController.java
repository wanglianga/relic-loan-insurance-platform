package com.relic.platform.controller;

import com.relic.platform.dto.ApiResponse;
import com.relic.platform.dto.DashboardStats;
import com.relic.platform.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("")
    public ApiResponse<DashboardStats> getStats() {
        try {
            DashboardStats stats = dashboardService.getStats();
            return ApiResponse.success(stats);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
