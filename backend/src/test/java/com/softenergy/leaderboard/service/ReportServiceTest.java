package com.softenergy.leaderboard.service;

import com.softenergy.leaderboard.api.BusinessException;
import com.softenergy.leaderboard.api.dto.ReportDtos;
import com.softenergy.leaderboard.domain.model.*;
import com.softenergy.leaderboard.domain.repository.ContentReportRepository;
import com.softenergy.leaderboard.domain.repository.MiniappUserRepository;
import com.softenergy.leaderboard.domain.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReportServiceTest {
    private ContentReportRepository repository;
    private MiniappUserRepository userRepository;
    private SubmissionRepository submissionRepository;
    private ActivityService activityService;
    private WechatContentSecurityService contentSecurityService;
    private ReportService service;

    @BeforeEach
    void setUp() {
        repository = mock(ContentReportRepository.class);
        userRepository = mock(MiniappUserRepository.class);
        submissionRepository = mock(SubmissionRepository.class);
        activityService = mock(ActivityService.class);
        contentSecurityService = mock(WechatContentSecurityService.class);
        service = new ReportService(repository, userRepository, submissionRepository,
                activityService, contentSecurityService, mock(AuditService.class));
        when(repository.save(any(ContentReport.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsQuestionReportWithServerResolvedTarget() {
        MiniappUser reporter = user("u1", "openid-1", "学员甲");
        Activity activity = new Activity();
        activity.setId("a1");
        activity.setTitle("知行合一");
        when(userRepository.findById("u1")).thenReturn(Optional.of(reporter));
        when(activityService.requirePublic("a1")).thenReturn(activity);

        var result = service.create("u1", new ReportDtos.CreateReportRequest(
                ReportType.QUESTION_CONTENT, "a1", null, null, "题目中的出处需要核实"));

        assertEquals(ReportTargetType.ACTIVITY, result.targetType());
        assertEquals("知行合一", result.targetLabel());
        assertEquals(ReportStatus.PENDING, result.status());
        verify(contentSecurityService).checkText("openid-1", "题目中的出处需要核实");
    }

    @Test
    void refusesSelfNicknameReport() {
        MiniappUser reporter = user("u1", "openid-1", "学员甲");
        when(userRepository.findById("u1")).thenReturn(Optional.of(reporter));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.create(
                "u1", new ReportDtos.CreateReportRequest(
                        ReportType.INAPPROPRIATE_NICKNAME, null, "u1", null, "我想举报自己的昵称")));

        assertEquals("REPORT_SELF_NOT_ALLOWED", exception.getCode());
        verify(repository, never()).save(any());
    }

    private MiniappUser user(String id, String openid, String nickname) {
        MiniappUser user = new MiniappUser();
        user.setId(id);
        user.setOpenid(openid);
        user.setNickname(nickname);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
