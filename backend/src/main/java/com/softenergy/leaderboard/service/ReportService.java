package com.softenergy.leaderboard.service;

import com.softenergy.leaderboard.api.BusinessException;
import com.softenergy.leaderboard.api.dto.ReportDtos;
import com.softenergy.leaderboard.domain.model.*;
import com.softenergy.leaderboard.domain.repository.ContentReportRepository;
import com.softenergy.leaderboard.domain.repository.MiniappUserRepository;
import com.softenergy.leaderboard.domain.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class ReportService {
    private static final Set<ReportStatus> OPEN_STATUSES = Set.of(ReportStatus.PENDING, ReportStatus.PROCESSING);
    private final ContentReportRepository repository;
    private final MiniappUserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final ActivityService activityService;
    private final WechatContentSecurityService contentSecurityService;
    private final AuditService auditService;

    public ReportService(
            ContentReportRepository repository,
            MiniappUserRepository userRepository,
            SubmissionRepository submissionRepository,
            ActivityService activityService,
            WechatContentSecurityService contentSecurityService,
            AuditService auditService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
        this.activityService = activityService;
        this.contentSecurityService = contentSecurityService;
        this.auditService = auditService;
    }

    @Transactional
    public ReportDtos.ReportItem create(String userId, ReportDtos.CreateReportRequest request) {
        MiniappUser reporter = requireActiveUser(userId);
        if (repository.countByReporterIdAndStatusIn(userId, OPEN_STATUSES) >= 5) {
            throw BusinessException.conflict("REPORT_LIMIT", "你已有多条反馈正在处理，请等待处理完成后再提交");
        }

        String description = request.description().trim();
        contentSecurityService.checkText(reporter.getOpenid(), description);
        Target target = resolveTarget(reporter, request);

        ContentReport report = new ContentReport();
        report.setReporter(reporter);
        report.setType(request.type());
        report.setTargetType(target.type());
        report.setTargetId(target.id());
        report.setTargetLabel(target.label());
        report.setDescription(description);
        report.setStatus(ReportStatus.PENDING);
        return toItem(repository.save(report));
    }

    @Transactional(readOnly = true)
    public List<ReportDtos.ReportItem> myReports(String userId) {
        requireActiveUser(userId);
        return repository.findByReporterIdOrderByCreatedAtDesc(userId).stream().map(this::toItem).toList();
    }

    @Transactional(readOnly = true)
    public List<ReportDtos.ReportItem> adminList(ReportStatus status) {
        List<ContentReport> reports = status == null
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findByStatusOrderByCreatedAtDesc(status);
        return reports.stream().map(this::toItem).toList();
    }

    @Transactional
    public ReportDtos.ReportItem handle(
            String id, ReportDtos.HandleReportRequest request, String admin) {
        if (request.status() == ReportStatus.PENDING) {
            throw BusinessException.badRequest("REPORT_STATUS_ERROR", "处理后不能重新设为待处理");
        }
        String note = request.note() == null ? "" : request.note().trim();
        if ((request.status() == ReportStatus.RESOLVED || request.status() == ReportStatus.REJECTED)
                && note.length() < 2) {
            throw BusinessException.badRequest("REPORT_NOTE_REQUIRED", "办结或驳回时请填写处理说明");
        }

        ContentReport report = repository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("反馈记录不存在"));
        applyModerationAction(report, request.action());
        report.setStatus(request.status());
        report.setHandlingNote(note.isBlank() ? null : note);
        report.setHandledBy(admin);
        report.setHandledAt(Instant.now());
        auditService.record(admin, "HANDLE_REPORT", "REPORT", report.getId(),
                "处理反馈工单，状态调整为 " + request.status());
        return toItem(report);
    }

    private Target resolveTarget(MiniappUser reporter, ReportDtos.CreateReportRequest request) {
        return switch (request.type()) {
            case QUESTION_CONTENT -> {
                Activity activity = requireActivity(request.activityId());
                yield new Target(ReportTargetType.ACTIVITY, activity.getId(), activity.getTitle());
            }
            case INAPPROPRIATE_NICKNAME -> {
                if (request.targetUserId() == null || request.targetUserId().isBlank()) {
                    throw BusinessException.badRequest("REPORT_TARGET_REQUIRED", "请选择需要举报的榜单昵称");
                }
                MiniappUser target = requireActiveUser(request.targetUserId());
                if (target.getId().equals(reporter.getId())) {
                    throw BusinessException.badRequest("REPORT_SELF_NOT_ALLOWED", "不能举报自己的昵称，可在个人资料中直接修改");
                }
                yield new Target(ReportTargetType.USER, target.getId(), target.getNickname());
            }
            case SCORE_APPEAL -> {
                if (request.submissionId() == null || request.submissionId().isBlank()) {
                    throw BusinessException.badRequest("REPORT_TARGET_REQUIRED", "缺少需要申诉的答题记录");
                }
                Submission submission = submissionRepository.findById(request.submissionId())
                        .filter(item -> item.getUser().getId().equals(reporter.getId()))
                        .orElseThrow(() -> BusinessException.notFound("答题记录不存在或不属于当前用户"));
                yield new Target(ReportTargetType.SUBMISSION, submission.getId(), submission.getActivity().getTitle());
            }
            case INFRINGEMENT -> {
                if (request.activityId() == null || request.activityId().isBlank()) {
                    yield new Target(ReportTargetType.GENERAL, null, "知识产权与侵权投诉");
                }
                Activity activity = requireActivity(request.activityId());
                yield new Target(ReportTargetType.ACTIVITY, activity.getId(), activity.getTitle());
            }
            case ILLEGAL_CONTENT, PRIVACY, OTHER -> new Target(ReportTargetType.GENERAL, null, "平台服务");
        };
    }

    private Activity requireActivity(String activityId) {
        if (activityId == null || activityId.isBlank()) {
            throw BusinessException.badRequest("REPORT_TARGET_REQUIRED", "缺少需要反馈的题目");
        }
        return activityService.requirePublic(activityId);
    }

    private void applyModerationAction(ContentReport report, ModerationAction action) {
        if (action == ModerationAction.NONE) return;
        if (report.getTargetType() != ReportTargetType.USER || report.getTargetId() == null) {
            throw BusinessException.badRequest("MODERATION_TARGET_ERROR", "当前反馈不支持账号处置");
        }
        MiniappUser target = userRepository.findById(report.getTargetId())
                .orElseThrow(() -> BusinessException.notFound("被举报账号不存在"));
        if (action == ModerationAction.RESET_NICKNAME) {
            target.setNickname("修习者");
            report.setTargetLabel("修习者");
        } else if (action == ModerationAction.DISABLE_USER) {
            target.setStatus(UserStatus.DISABLED);
        }
    }

    private MiniappUser requireActiveUser(String id) {
        return userRepository.findById(id)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.notFound("用户不存在或已停用"));
    }

    private ReportDtos.ReportItem toItem(ContentReport report) {
        String reporterName = report.getReporter() == null ? "已注销用户" : report.getReporter().getNickname();
        return new ReportDtos.ReportItem(
                report.getId(), report.getType(), report.getTargetType(), report.getTargetId(),
                report.getTargetLabel(), reporterName, report.getDescription(), report.getStatus(),
                report.getHandlingNote(), report.getHandledBy(), report.getHandledAt(),
                report.getCreatedAt(), report.getUpdatedAt());
    }

    private record Target(ReportTargetType type, String id, String label) {}
}
