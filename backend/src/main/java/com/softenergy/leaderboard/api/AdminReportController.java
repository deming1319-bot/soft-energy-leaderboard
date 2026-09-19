package com.softenergy.leaderboard.api;

import com.softenergy.leaderboard.api.dto.ReportDtos;
import com.softenergy.leaderboard.domain.model.ReportStatus;
import com.softenergy.leaderboard.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/reports")
public class AdminReportController {
    private final ReportService service;

    public AdminReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<ReportDtos.ReportItem>> list(
            @RequestParam(required = false) ReportStatus status) {
        return ApiResponse.success(service.adminList(status));
    }

    @PostMapping("/{id}/handle")
    public ApiResponse<ReportDtos.ReportItem> handle(
            @PathVariable String id,
            @Valid @RequestBody ReportDtos.HandleReportRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success("反馈处理结果已保存", service.handle(id, request, jwt.getSubject()));
    }
}
