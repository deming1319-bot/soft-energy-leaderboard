package com.softenergy.leaderboard.api;

import com.softenergy.leaderboard.api.dto.SubmissionDtos;
import com.softenergy.leaderboard.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/submissions")
public class AdminSubmissionController {
    private final SubmissionService service;

    public AdminSubmissionController(SubmissionService service) {
        this.service = service;
    }

    @PostMapping("/{id}/review")
    public ApiResponse<SubmissionDtos.AdminSubmissionItem> review(
            @PathVariable String id,
            @Valid @RequestBody SubmissionDtos.ReviewSubmissionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success("复核结果已保存", service.review(id, request, jwt.getSubject()));
    }
}

