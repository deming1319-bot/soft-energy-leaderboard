package com.softenergy.leaderboard.api.dto;

import com.softenergy.leaderboard.domain.model.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ActivityDtos {
    private ActivityDtos() {}

    public record UpsertActivityRequest(
            @NotBlank(message = "请输入活动名称") @Size(max = 120) String title,
            @Size(max = 240) String subtitle,
            @NotNull(message = "请选择文化分类") CultureCategory category,
            @NotBlank(message = "请输入题目内容") String questionText,
            @Size(max = 160) String sourceTitle,
            @Size(max = 500) String sourceDetail,
            @NotBlank(message = "请输入标准答案") String standardAnswer,
            @NotEmpty(message = "请至少设置一个答案要点") List<@NotBlank @Size(max = 300) String> answerPoints,
            @NotNull @DecimalMin("0.3") @DecimalMax("1.0") BigDecimal runnerUpThreshold,
            DeadlineMode deadlineMode,
            @Min(1) @Max(720) Integer durationHours,
            @NotNull(message = "请选择开始时间") Instant startAt,
            @NotNull(message = "请选择截止时间") Instant endAt,
            @NotNull RevealMode revealMode,
            @Min(1) @Max(5) int maxAttempts) {}

    public record AdminActivitySummary(
            String id,
            String title,
            String subtitle,
            CultureCategory category,
            ActivityStatus status,
            String timeState,
            Instant startAt,
            Instant endAt,
            long submissionCount,
            long championCount,
            long runnerUpCount,
            Instant updatedAt) {}

    public record AdminActivityDetail(
            String id,
            String title,
            String subtitle,
            CultureCategory category,
            String questionText,
            String sourceTitle,
            String sourceDetail,
            String standardAnswer,
            List<String> answerPoints,
            BigDecimal runnerUpThreshold,
            DeadlineMode deadlineMode,
            Integer durationHours,
            Instant startAt,
            Instant endAt,
            ActivityStatus status,
            RevealMode revealMode,
            int maxAttempts,
            Instant publishedAt,
            Instant closedAt,
            long version) {}

    public record PublicActivitySummary(
            String id,
            String title,
            String subtitle,
            CultureCategory category,
            String timeState,
            Instant startAt,
            Instant endAt,
            long participantCount,
            boolean submitted) {}

    public record PublicActivityDetail(
            String id,
            String title,
            String subtitle,
            CultureCategory category,
            String questionText,
            String sourceTitle,
            String sourceDetail,
            String timeState,
            Instant startAt,
            Instant endAt,
            int maxAttempts,
            int attemptsUsed,
            boolean canSubmit,
            boolean submitted) {}
}
