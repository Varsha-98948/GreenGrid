package com.greengrid.controller;

import com.greengrid.dto.common.ApiResponse;
import com.greengrid.dto.dashboard.DashboardResponse;
import com.greengrid.security.UserPrincipal;
import com.greengrid.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DateTimeException;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * @param timezone IANA zone id (e.g. "Asia/Kolkata"), sent by the
     *                 frontend as Intl.DateTimeFormat().resolvedOptions().timeZone.
     *                 Falls back to the server's default zone if omitted or
     *                 unrecognized, so the endpoint degrades gracefully
     *                 rather than failing for older/non-browser clients.
     */
    @GetMapping
    public ApiResponse<DashboardResponse> getDashboard(@AuthenticationPrincipal UserPrincipal principal,
                                                        @RequestParam(required = false) String timezone) {
        ZoneId zone = resolveZone(timezone);
        return ApiResponse.ok(dashboardService.buildDashboard(principal.getId(), zone));
    }

    private ZoneId resolveZone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException ex) {
            return ZoneId.systemDefault();
        }
    }
}
