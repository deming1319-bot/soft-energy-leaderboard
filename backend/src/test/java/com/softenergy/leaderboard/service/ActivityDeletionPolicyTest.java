package com.softenergy.leaderboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softenergy.leaderboard.api.BusinessException;
import com.softenergy.leaderboard.domain.model.Activity;
import com.softenergy.leaderboard.domain.model.ActivityStatus;
import com.softenergy.leaderboard.domain.repository.ActivityRepository;
import com.softenergy.leaderboard.domain.repository.LeaderboardSnapshotRepository;
import com.softenergy.leaderboard.domain.repository.SubmissionRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ActivityDeletionPolicyTest {
    @Test
    void deletesANeverPublishedDraftAndRecordsTheAuditEvent() {
        ActivityRepository repository = mock(ActivityRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        LeaderboardSnapshotRepository snapshotRepository = mock(LeaderboardSnapshotRepository.class);
        AuditService auditService = mock(AuditService.class);
        Activity activity = activity("draft-1", ActivityStatus.DRAFT, null);
        when(repository.findById("draft-1")).thenReturn(Optional.of(activity));
        ActivityService service = service(repository, submissionRepository, snapshotRepository, auditService);

        service.deleteDraft("draft-1", "admin");

        verify(repository).delete(activity);
        verify(auditService).record(
                "admin", "DELETE_ACTIVITY", "ACTIVITY", "draft-1", "删除草稿活动“待删除题目”");
    }

    @Test
    void refusesToDeleteAnActivityThatHasAlreadyBeenPublished() {
        ActivityRepository repository = mock(ActivityRepository.class);
        Activity activity = activity("published-1", ActivityStatus.PUBLISHED, Instant.now());
        when(repository.findById("published-1")).thenReturn(Optional.of(activity));
        ActivityService service = service(
                repository, mock(SubmissionRepository.class), mock(LeaderboardSnapshotRepository.class),
                mock(AuditService.class));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.deleteDraft("published-1", "admin"));

        assertEquals("ACTIVITY_DELETE_FORBIDDEN", exception.getCode());
        verify(repository, never()).delete(any());
    }

    @Test
    void refusesToDeleteADraftThatAlreadyHasResults() {
        ActivityRepository repository = mock(ActivityRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        Activity activity = activity("draft-with-results", ActivityStatus.DRAFT, null);
        when(repository.findById("draft-with-results")).thenReturn(Optional.of(activity));
        when(submissionRepository.countByActivityId("draft-with-results")).thenReturn(1L);
        ActivityService service = service(
                repository, submissionRepository, mock(LeaderboardSnapshotRepository.class), mock(AuditService.class));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.deleteDraft("draft-with-results", "admin"));

        assertEquals("ACTIVITY_HAS_RESULTS", exception.getCode());
        verify(repository, never()).delete(any());
    }

    private ActivityService service(
            ActivityRepository repository,
            SubmissionRepository submissionRepository,
            LeaderboardSnapshotRepository snapshotRepository,
            AuditService auditService) {
        return new ActivityService(
                repository, submissionRepository, snapshotRepository, new ObjectMapper(), auditService);
    }

    private Activity activity(String id, ActivityStatus status, Instant publishedAt) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setTitle("待删除题目");
        activity.setStatus(status);
        activity.setPublishedAt(publishedAt);
        return activity;
    }
}
