package com.softenergy.leaderboard.api;

import com.softenergy.leaderboard.api.dto.ActivityDtos;
import com.softenergy.leaderboard.api.dto.SubmissionDtos;
import com.softenergy.leaderboard.service.ActivityService;
import com.softenergy.leaderboard.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/activities")
public class AdminActivityController {
    private final ActivityService activityService;
    private final SubmissionService submissionService;

    public AdminActivityController(ActivityService activityService, SubmissionService submissionService) {
        this.activityService = activityService;
        this.submissionService = submissionService;
    }

    @GetMapping
    public ApiResponse<List<ActivityDtos.AdminActivitySummary>> list() {
        return ApiResponse.success(activityService.adminList());
    }

    @GetMapping("/{id}")
    public ApiResponse<ActivityDtos.AdminActivityDetail> detail(@PathVariable String id) {
        return ApiResponse.success(activityService.adminDetail(id));
    }

    @PostMapping
    public ApiResponse<ActivityDtos.AdminActivityDetail> create(
            @Valid @RequestBody ActivityDtos.UpsertActivityRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success("活动已保存为草稿", activityService.create(request, jwt.getSubject()));
    }

    @PutMapping("/{id}")
    public ApiResponse<ActivityDtos.AdminActivityDetail> update(
            @PathVariable String id,
            @Valid @RequestBody ActivityDtos.UpsertActivityRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success("活动已更新", activityService.update(id, request, jwt.getSubject()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDraft(
            @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        activityService.deleteDraft(id, jwt.getSubject());
        return ApiResponse.success("草稿题目已删除", null);
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<ActivityDtos.AdminActivityDetail> publish(
            @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success("活动已发布", activityService.publish(id, jwt.getSubject()));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<ActivityDtos.AdminActivityDetail> close(
            @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success("活动已结束，榜单已经冻结", activityService.close(id, jwt.getSubject()));
    }

    @GetMapping("/{id}/submissions")
    public ApiResponse<List<SubmissionDtos.AdminSubmissionItem>> submissions(@PathVariable String id) {
        return ApiResponse.success(submissionService.adminList(id));
    }

    @GetMapping("/{id}/leaderboard")
    public ApiResponse<SubmissionDtos.LeaderboardResponse> leaderboard(@PathVariable String id) {
        return ApiResponse.success(submissionService.leaderboard(id, null, true));
    }
}
