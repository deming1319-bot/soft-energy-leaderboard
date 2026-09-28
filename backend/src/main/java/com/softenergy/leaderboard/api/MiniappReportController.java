package com.softenergy.leaderboard.api;

import com.softenergy.leaderboard.api.dto.ReportDtos;
import com.softenergy.leaderboard.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/miniapp/reports")
public class MiniappReportController {
    private final ReportService service;

    public MiniappReportController(ReportService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<ReportDtos.ReportItem> create(
            @Valid @RequestBody ReportDtos.CreateReportRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success("反馈已提交，我们会尽快处理", service.create(jwt.getSubject(), request));
    }

    @GetMapping("/mine")
    public ApiResponse<List<ReportDtos.ReportItem>> mine(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(service.myReports(jwt.getSubject()));
    }
}
