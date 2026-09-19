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
@RequestMapping("/api/v1/miniapp/activities")
public class MiniappActivityController {
    private final ActivityService activityService;
    private final SubmissionService submissionService;

    public MiniappActivityController(ActivityService activityService, SubmissionService submissionService) {
        this.activityService = activityService;
        this.submissionService = submissionService;
    }

    @GetMapping
    public ApiResponse<List<ActivityDtos.PublicActivitySummary>> list(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(activityService.publicList(jwt.getSubject()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ActivityDtos.PublicActivityDetail> detail(
            @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(activityService.publicDetail(id, jwt.getSubject()));
    }

    @PostMapping("/{id}/submissions")
    public ApiResponse<SubmissionDtos.SubmissionResult> submit(
            @PathVariable String id,
            @Valid @RequestBody SubmissionDtos.SubmitAnswerRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success("答案已提交", submissionService.submit(id, jwt.getSubject(), request));
    }

    @GetMapping("/{id}/my-submission")
    public ApiResponse<SubmissionDtos.SubmissionResult> mySubmission(
            @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(submissionService.myLatest(id, jwt.getSubject()));
    }

    @GetMapping("/{id}/leaderboard")
    public ApiResponse<SubmissionDtos.LeaderboardResponse> leaderboard(
            @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(submissionService.leaderboard(id, jwt.getSubject(), false));
    }
}

