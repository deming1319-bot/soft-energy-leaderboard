package com.softenergy.leaderboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softenergy.leaderboard.domain.model.Activity;
import com.softenergy.leaderboard.domain.model.ActivityStatus;
import com.softenergy.leaderboard.domain.model.CultureCategory;
import com.softenergy.leaderboard.domain.repository.ActivityRepository;
import com.softenergy.leaderboard.domain.repository.LeaderboardSnapshotRepository;
import com.softenergy.leaderboard.domain.repository.SubmissionRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

class ActivityGuestAccessTest {
    @Test
    void guestCanBrowsePublishedListWithoutUserLookup() {
        ActivityRepository repository = mock(ActivityRepository.class);
        SubmissionRepository submissions = mock(SubmissionRepository.class);
        LeaderboardSnapshotRepository snapshots = mock(LeaderboardSnapshotRepository.class);
        Activity activity = publishedActivity();
        when(repository.findByStatusInOrderByStartAtDesc(anySet())).thenReturn(List.of(activity));
        when(submissions.countByActivityId(activity.getId())).thenReturn(2L);
        when(snapshots.countByActivityId(activity.getId())).thenReturn(1L);
        ActivityService service = service(repository, submissions, snapshots);

        var result = service.publicList(null);

        assertEquals(1, result.size());
        assertEquals(3L, result.getFirst().participantCount());
        assertFalse(result.getFirst().submitted());
        verify(submissions, never()).findFirstByActivityIdAndUserIdOrderByAttemptNoDesc(anyString(), anyString());
    }

    @Test
    void guestCanReadQuestionButCannotSubmitWithoutLogin() {
        ActivityRepository repository = mock(ActivityRepository.class);
        SubmissionRepository submissions = mock(SubmissionRepository.class);
        LeaderboardSnapshotRepository snapshots = mock(LeaderboardSnapshotRepository.class);
        Activity activity = publishedActivity();
        when(repository.findById(activity.getId())).thenReturn(Optional.of(activity));
        when(submissions.countByActivityId(activity.getId())).thenReturn(4L);
        ActivityService service = service(repository, submissions, snapshots);

        var detail = service.publicDetail(activity.getId(), null);

        assertEquals("请写出你的理解", detail.questionText());
        assertEquals(4L, detail.participantCount());
        assertEquals(0, detail.attemptsUsed());
        assertFalse(detail.canSubmit());
        assertFalse(detail.submitted());
        verify(submissions, never()).findByActivityIdOrderBySubmittedAtAsc(anyString());
    }

    private ActivityService service(
            ActivityRepository repository,
            SubmissionRepository submissions,
            LeaderboardSnapshotRepository snapshots) {
        return new ActivityService(
                repository, submissions, snapshots, new ObjectMapper(), mock(AuditService.class));
    }

    private Activity publishedActivity() {
        Activity activity = new Activity();
        activity.setId("guest-visible");
        activity.setTitle("公开题目");
        activity.setQuestionText("请写出你的理解");
        activity.setCategory(CultureCategory.CLASSICS);
        activity.setStatus(ActivityStatus.PUBLISHED);
        activity.setStartAt(Instant.now().minus(1, ChronoUnit.HOURS));
        activity.setEndAt(Instant.now().plus(1, ChronoUnit.HOURS));
        activity.setMaxAttempts(1);
        return activity;
    }
}
