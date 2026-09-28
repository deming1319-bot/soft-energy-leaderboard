package com.softenergy.leaderboard.api.dto;

import com.softenergy.leaderboard.domain.model.SubmissionGrade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class SubmissionDtos {
    private SubmissionDtos() {}

    public record SubmitAnswerRequest(
            @NotBlank(message = "请填写答案") @Size(max = 5000) String answer,
            @NotBlank(message = "缺少请求标识") @Size(max = 64) String clientRequestId) {}

    public record SubmissionResult(
            String id,
            String activityId,
            String activityTitle,
            SubmissionGrade grade,
            BigDecimal score,
            List<String> matchedPoints,
            Instant submittedAt,
            String feedback,
            boolean answerRevealed,
            String standardAnswer,
            String reviewNote) {}

    public record AdminSubmissionItem(
            String id,
            String userId,
            String userDisplayName,
            String maskedPhone,
            int attemptNo,
            String answerText,
            SubmissionGrade originalGrade,
            SubmissionGrade grade,
            BigDecimal score,
            List<String> matchedPoints,
            Instant submittedAt,
            String reviewedBy,
            Instant reviewedAt,
            String reviewNote) {}

    public record ReviewSubmissionRequest(
            @NotNull(message = "请选择复核结果") SubmissionGrade grade,
            @NotBlank(message = "请填写复核说明") @Size(max = 500) String note) {}

    public record LeaderboardEntry(
            int rank,
            String userId,
            String displayName,
            String avatarUrl,
            SubmissionGrade grade,
            Instant submittedAt,
            boolean currentUser) {}

    public record LeaderboardResponse(
            String activityId,
            String activityTitle,
            boolean revealed,
            Instant endAt,
            List<LeaderboardEntry> champions,
            List<LeaderboardEntry> runnersUp,
            SubmissionResult myResult) {}
}

