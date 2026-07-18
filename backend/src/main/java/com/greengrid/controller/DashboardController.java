package com.greengrid.controller;

import com.greengrid.dto.common.ApiResponse;
import com.greengrid.dto.dashboard.DashboardResponse;
import com.greengrid.security.UserPrincipal;
import com.greengrid.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ApiResponse<DashboardResponse> getDashboard(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(dashboardService.buildDashboard(principal.getId()));
    }
}
