package com.greengrid.controller;

import com.greengrid.dto.common.ApiResponse;
import com.greengrid.dto.settings.AccountSummaryResponse;
import com.greengrid.dto.settings.ExportDataResponse;
import com.greengrid.dto.settings.UpdateThemeRequest;
import com.greengrid.security.UserPrincipal;
import com.greengrid.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/account")
    public ApiResponse<AccountSummaryResponse> getAccount(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(settingsService.getAccountSummary(principal.getId()));
    }

    @PatchMapping("/theme")
    public ApiResponse<Void> updateTheme(@AuthenticationPrincipal UserPrincipal principal,
                                          @Valid @RequestBody UpdateThemeRequest request) {
        settingsService.updateTheme(principal.getId(), request.theme());
        return ApiResponse.message("Theme updated");
    }

    @GetMapping("/export")
    public ApiResponse<ExportDataResponse> exportData(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(settingsService.exportData(principal.getId()));
    }

    @DeleteMapping("/account")
    public ApiResponse<Void> deleteAccount(@AuthenticationPrincipal UserPrincipal principal) {
        settingsService.deleteAccount(principal.getId());
        return ApiResponse.message("Account deleted");
    }
}
