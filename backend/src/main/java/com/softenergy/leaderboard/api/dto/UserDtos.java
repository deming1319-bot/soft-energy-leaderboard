package com.softenergy.leaderboard.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class UserDtos {
    private UserDtos() {}

    public record UserListItem(
            String id,
            String nickname,
            String avatarUrl,
            String maskedPhone,
            String status,
            long participationCount,
            long championCount,
            long runnerUpCount,
            Instant lastLoginAt,
            Instant createdAt) {}

    public record MiniappProfile(
            String id,
            String nickname,
            String avatarUrl,
            String maskedPhone,
            boolean profileComplete,
            String privacyVersion,
            String termsVersion,
            Instant consentedAt,
            long participationCount,
            long championCount,
            long runnerUpCount,
            List<SubmissionDtos.SubmissionResult> recentResults) {}

    public record UpdateMiniappProfileRequest(
            @NotBlank(message = "请输入修习名")
            @Size(min = 2, max = 16, message = "修习名需为2至16个字")
            @Pattern(regexp = "^[\\p{L}\\p{N}·._ -]+$", message = "修习名只能包含文字、数字、空格、间隔点、横线或下划线")
            String nickname) {}
}
