package com.softenergy.leaderboard.api.dto;

import java.time.Instant;
import java.util.List;

public final class DashboardDtos {
    private DashboardDtos() {}

    public record DashboardOverview(
            long totalUsers,
            long activeActivities,
            long totalSubmissions,
            long championCount,
            long runnerUpCount,
            long reviewRequiredCount,
            List<RecentSubmission> recentSubmissions) {}

    public record RecentSubmission(
            String id,
            String activityTitle,
            String userDisplayName,
            String grade,
            Instant submittedAt) {}
}

