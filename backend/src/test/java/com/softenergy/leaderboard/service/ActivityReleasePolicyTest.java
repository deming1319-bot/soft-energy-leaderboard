package com.softenergy.leaderboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softenergy.leaderboard.api.BusinessException;
import com.softenergy.leaderboard.domain.model.Activity;
import com.softenergy.leaderboard.domain.model.ActivityStatus;
import com.softenergy.leaderboard.domain.model.CultureCategory;
import com.softenergy.leaderboard.domain.model.DeadlineMode;
import com.softenergy.leaderboard.domain.repository.ActivityRepository;
import com.softenergy.leaderboard.domain.repository.LeaderboardSnapshotRepository;
import com.softenergy.leaderboard.domain.repository.SubmissionRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ActivityReleasePolicyTest {
    @Test
    void blocksUnqualifiedReligiousCategoryFromFirstRelease() {
        ActivityRepository repository = mock(ActivityRepository.class);
        Activity activity = new Activity();
        activity.setId("a1");
        activity.setTitle("测试题目");
        activity.setCategory(CultureCategory.BUDDHIST);
        activity.setStatus(ActivityStatus.DRAFT);
        activity.setEndAt(Instant.now().plus(2, ChronoUnit.HOURS));
        when(repository.findById("a1")).thenReturn(Optional.of(activity));
        ActivityService service = new ActivityService(
                repository, mock(SubmissionRepository.class), mock(LeaderboardSnapshotRepository.class),
                new ObjectMapper(), mock(AuditService.class));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.publish("a1", "admin"));

        assertEquals("PUBLISH_CONTENT_RESTRICTED", exception.getCode());
        assertEquals(ActivityStatus.DRAFT, activity.getStatus());
    }

    @Test
    void durationDeadlineStartsAtTheActualPublishMoment() {
        ActivityRepository repository = mock(ActivityRepository.class);
        Activity activity = new Activity();
        activity.setId("duration-12h");
        activity.setTitle("对联测试题");
        activity.setQuestionText("请写出上下联");
        activity.setStandardAnswer("海纳百川，有容乃大\n壁立千仞，无欲则刚");
        activity.setCategory(CultureCategory.CLASSICS);
        activity.setStatus(ActivityStatus.DRAFT);
        activity.setDeadlineMode(DeadlineMode.DURATION);
        activity.setDurationHours(12);
        activity.setStartAt(Instant.now().minus(3, ChronoUnit.DAYS));
        activity.setEndAt(Instant.now().minus(2, ChronoUnit.DAYS));
        when(repository.findById("duration-12h")).thenReturn(Optional.of(activity));
        ActivityService service = new ActivityService(
                repository, mock(SubmissionRepository.class), mock(LeaderboardSnapshotRepository.class),
                new ObjectMapper(), mock(AuditService.class));

        Instant beforePublish = Instant.now();
        service.publish("duration-12h", "admin");

        assertEquals(ActivityStatus.PUBLISHED, activity.getStatus());
        assertEquals(activity.getPublishedAt(), activity.getStartAt());
        assertEquals(12, ChronoUnit.HOURS.between(activity.getStartAt(), activity.getEndAt()));
        assertTrue(!activity.getPublishedAt().isBefore(beforePublish));
    }
}
