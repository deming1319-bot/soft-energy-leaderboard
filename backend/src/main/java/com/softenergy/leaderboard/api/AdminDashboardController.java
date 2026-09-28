package com.softenergy.leaderboard.api;

import com.softenergy.leaderboard.api.dto.DashboardDtos;
import com.softenergy.leaderboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class AdminDashboardController {
    private final DashboardService service;

    public AdminDashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<DashboardDtos.DashboardOverview> overview() {
        return ApiResponse.success(service.overview());
    }
}

