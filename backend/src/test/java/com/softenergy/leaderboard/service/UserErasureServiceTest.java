package com.softenergy.leaderboard.service;

import com.softenergy.leaderboard.domain.model.*;
import com.softenergy.leaderboard.domain.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserErasureServiceTest {
    @Test
    void cancellationDeletesPersonalRecordsAndKeepsOnlyOneAnonymousHonorSnapshot() {
        MiniappUserRepository userRepository = mock(MiniappUserRepository.class);
        SubmissionRepository submissionRepository = mock(SubmissionRepository.class);
        LeaderboardSnapshotRepository snapshotRepository = mock(LeaderboardSnapshotRepository.class);
        ContentReportRepository reportRepository = mock(ContentReportRepository.class);
        MiniappUser user = new MiniappUser();
        user.setId("u1");
        user.setOpenid("openid-1");
        user.setNickname("学员甲");
        user.setStatus(UserStatus.ACTIVE);
        Activity activity = new Activity();
        activity.setId("a1");
        activity.setTitle("知行合一");
        Submission runner = submission("s1", activity, user, SubmissionGrade.RUNNER_UP, Instant.parse("2026-08-02T01:00:00Z"));
        Submission champion = submission("s2", activity, user, SubmissionGrade.CHAMPION, Instant.parse("2026-08-02T01:05:00Z"));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(submissionRepository.findByUserIdOrderBySubmittedAtDesc("u1")).thenReturn(List.of(champion, runner));

        UserService service = new UserService(
                userRepository, submissionRepository, mock(PhoneCryptoService.class),
                mock(SubmissionService.class), mock(WechatContentSecurityService.class),
                snapshotRepository, reportRepository, mock(AuditService.class));
        service.cancelAccount("u1");

        ArgumentCaptor<LeaderboardSnapshot> captor = ArgumentCaptor.forClass(LeaderboardSnapshot.class);
        verify(snapshotRepository).save(captor.capture());
        assertEquals("已注销用户", captor.getValue().getDisplayName());
        assertEquals(SubmissionGrade.CHAMPION, captor.getValue().getGrade());
        verify(submissionRepository).deleteAll(List.of(champion, runner));
        verify(submissionRepository).flush();
        verify(reportRepository).deleteByReporterId("u1");
        verify(reportRepository).deleteByTargetTypeAndTargetId(ReportTargetType.USER, "u1");
        verify(userRepository).delete(user);
    }

    private Submission submission(
            String id, Activity activity, MiniappUser user, SubmissionGrade grade, Instant submittedAt) {
        Submission submission = new Submission();
        submission.setId(id);
        submission.setActivity(activity);
        submission.setUser(user);
        submission.setGrade(grade);
        submission.setSubmittedAt(submittedAt);
        return submission;
    }
}
