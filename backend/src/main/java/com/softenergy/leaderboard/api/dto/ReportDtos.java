package com.softenergy.leaderboard.api.dto;

import com.softenergy.leaderboard.domain.model.ModerationAction;
import com.softenergy.leaderboard.domain.model.ReportStatus;
import com.softenergy.leaderboard.domain.model.ReportTargetType;
import com.softenergy.leaderboard.domain.model.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ReportDtos {
    private ReportDtos() {}

    public record CreateReportRequest(
            @NotNull(message = "请选择反馈类型") ReportType type,
            @Size(max = 64, message = "题目标识格式不正确") String activityId,
            @Size(max = 64, message = "用户标识格式不正确") String targetUserId,
            @Size(max = 64, message = "答题标识格式不正确") String submissionId,
            @NotBlank(message = "请说明具体情况")
            @Size(min = 5, max = 1000, message = "情况说明需为5至1000个字") String description) {}

    public record HandleReportRequest(
            @NotNull(message = "请选择处理状态") ReportStatus status,
            @NotNull(message = "请选择处置动作") ModerationAction action,
            @Size(max = 500, message = "处理说明不能超过500个字") String note) {}

    public record ReportItem(
            String id,
            ReportType type,
            ReportTargetType targetType,
            String targetId,
            String targetLabel,
            String reporterDisplayName,
            String description,
            ReportStatus status,
            String handlingNote,
            String handledBy,
            Instant handledAt,
            Instant createdAt,
            Instant updatedAt) {}
}
