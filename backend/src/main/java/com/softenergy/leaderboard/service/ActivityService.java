package com.softenergy.leaderboard.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softenergy.leaderboard.api.BusinessException;
import com.softenergy.leaderboard.api.dto.ActivityDtos;
import com.softenergy.leaderboard.domain.model.*;
import com.softenergy.leaderboard.domain.repository.ActivityRepository;
import com.softenergy.leaderboard.domain.repository.SubmissionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
public class ActivityService {
    private static final Set<ActivityStatus> PUBLIC_STATUSES = Set.of(ActivityStatus.PUBLISHED, ActivityStatus.CLOSED);
    private static final Set<CultureCategory> RELEASE_CATEGORIES = Set.of(
            CultureCategory.CLASSICS, CultureCategory.PHILOSOPHY,
            CultureCategory.LIFE_PRACTICE, CultureCategory.CONFUCIAN);
    private static final List<String> RELEASE_RESTRICTED_TERMS = List.of(
            "佛教", "佛法", "释迦", "道教", "宗教", "皈依", "诵经", "讲经", "讲道",
            "师父", "法门", "寺院", "道观", "烧香", "拜佛", "受戒", "募捐");
    private final ActivityRepository repository;
    private final SubmissionRepository submissionRepository;
    private final com.softenergy.leaderboard.domain.repository.LeaderboardSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public ActivityService(
            ActivityRepository repository,
            SubmissionRepository submissionRepository,
            com.softenergy.leaderboard.domain.repository.LeaderboardSnapshotRepository snapshotRepository,
            ObjectMapper objectMapper,
            AuditService auditService) {
        this.repository = repository;
        this.submissionRepository = submissionRepository;
        this.snapshotRepository = snapshotRepository;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ActivityDtos.AdminActivitySummary> adminList() {
        return repository.findAll().stream()
                .sorted((left, right) -> right.getStartAt().compareTo(left.getStartAt()))
                .map(this::toAdminSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ActivityDtos.AdminActivityDetail adminDetail(String id) {
        return toAdminDetail(require(id));
    }

    @Transactional
    public ActivityDtos.AdminActivityDetail create(ActivityDtos.UpsertActivityRequest request, String admin) {
        validateTime(request);
        Activity activity = new Activity();
        apply(activity, request);
        Activity saved = repository.save(activity);
        auditService.record(admin, "CREATE_ACTIVITY", "ACTIVITY", saved.getId(), "创建活动“" + saved.getTitle() + "”");
        return toAdminDetail(saved);
    }

    @Transactional
    public ActivityDtos.AdminActivityDetail update(
            String id, ActivityDtos.UpsertActivityRequest request, String admin) {
        validateTime(request);
        Activity activity = require(id);
        if (activity.getStatus() != ActivityStatus.DRAFT) {
            throw BusinessException.conflict("ACTIVITY_LOCKED", "已发布活动不能直接修改，请先复制为新活动");
        }
        apply(activity, request);
        auditService.record(admin, "UPDATE_ACTIVITY", "ACTIVITY", id, "更新活动“" + activity.getTitle() + "”");
        return toAdminDetail(activity);
    }

    @Transactional
    public void deleteDraft(String id, String admin) {
        Activity activity = require(id);
        if (activity.getStatus() != ActivityStatus.DRAFT || activity.getPublishedAt() != null) {
            throw BusinessException.conflict("ACTIVITY_DELETE_FORBIDDEN", "只有从未发布的草稿题目可以删除");
        }
        if (submissionRepository.countByActivityId(id) > 0 || snapshotRepository.countByActivityId(id) > 0) {
            throw BusinessException.conflict("ACTIVITY_HAS_RESULTS", "题目已产生答题或榜单数据，不能删除");
        }
        String title = activity.getTitle();
        repository.delete(activity);
        auditService.record(admin, "DELETE_ACTIVITY", "ACTIVITY", id, "删除草稿活动“" + title + "”");
    }

    @Transactional
    public ActivityDtos.AdminActivityDetail publish(String id, String admin) {
        Activity activity = require(id);
        if (activity.getStatus() != ActivityStatus.DRAFT) {
            throw BusinessException.conflict("ACTIVITY_STATUS_ERROR", "只有草稿活动可以发布");
        }
        Instant publishedAt = Instant.now();
        if (activity.getDeadlineMode() == DeadlineMode.DURATION) {
            int durationHours = requireDurationHours(activity.getDurationHours());
            activity.setStartAt(publishedAt);
            activity.setEndAt(publishedAt.plus(durationHours, ChronoUnit.HOURS));
        }
        if (!activity.getEndAt().isAfter(publishedAt)) {
            throw BusinessException.badRequest("ACTIVITY_TIME_ERROR", "截止时间已过去，不能发布");
        }
        validateReleaseContent(activity);
        activity.setStatus(ActivityStatus.PUBLISHED);
        activity.setPublishedAt(publishedAt);
        auditService.record(admin, "PUBLISH_ACTIVITY", "ACTIVITY", id, "发布活动“" + activity.getTitle() + "”");
        return toAdminDetail(activity);
    }

    @Transactional
    public ActivityDtos.AdminActivityDetail close(String id, String admin) {
        Activity activity = require(id);
        if (activity.getStatus() != ActivityStatus.PUBLISHED) {
            throw BusinessException.conflict("ACTIVITY_STATUS_ERROR", "只有已发布活动可以结束");
        }
        activity.setStatus(ActivityStatus.CLOSED);
        activity.setClosedAt(Instant.now());
        auditService.record(admin, "CLOSE_ACTIVITY", "ACTIVITY", id, "结束活动“" + activity.getTitle() + "”");
        return toAdminDetail(activity);
    }

    @Transactional(readOnly = true)
    public List<ActivityDtos.PublicActivitySummary> publicList(String userId) {
        return repository.findByStatusInOrderByStartAtDesc(PUBLIC_STATUSES).stream()
                .filter(activity -> RELEASE_CATEGORIES.contains(activity.getCategory()))
                .map(activity -> new ActivityDtos.PublicActivitySummary(
                        activity.getId(), activity.getTitle(), activity.getSubtitle(), activity.getCategory(),
                        timeState(activity), activity.getStartAt(), activity.getEndAt(),
                        submissionRepository.countByActivityId(activity.getId())
                                + snapshotRepository.countByActivityId(activity.getId()),
                        submissionRepository.findFirstByActivityIdAndUserIdOrderByAttemptNoDesc(
                                activity.getId(), userId).isPresent()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ActivityDtos.PublicActivityDetail publicDetail(String id, String userId) {
        Activity activity = requirePublic(id);
        int attempts = submissionRepository.findByActivityIdOrderBySubmittedAtAsc(id).stream()
                .filter(submission -> submission.getUser().getId().equals(userId))
                .toList().size();
        boolean submitted = attempts > 0;
        boolean canSubmit = "ACTIVE".equals(timeState(activity)) && attempts < activity.getMaxAttempts();
        return new ActivityDtos.PublicActivityDetail(
                activity.getId(), activity.getTitle(), activity.getSubtitle(), activity.getCategory(),
                activity.getQuestionText(), activity.getSourceTitle(), activity.getSourceDetail(),
                timeState(activity), activity.getStartAt(), activity.getEndAt(), activity.getMaxAttempts(),
                attempts, canSubmit, submitted);
    }

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void closeExpiredActivities() {
        Instant now = Instant.now();
        repository.findByStatusAndEndAtBefore(ActivityStatus.PUBLISHED, now).forEach(activity -> {
            activity.setStatus(ActivityStatus.CLOSED);
            activity.setClosedAt(now);
        });
    }

    public Activity require(String id) {
        return repository.findById(id).orElseThrow(() -> BusinessException.notFound("活动不存在"));
    }

    public Activity requirePublic(String id) {
        Activity activity = require(id);
        if (!PUBLIC_STATUSES.contains(activity.getStatus()) || !RELEASE_CATEGORIES.contains(activity.getCategory())) {
            throw BusinessException.notFound("活动不存在或尚未发布");
        }
        return activity;
    }

    public List<String> parsePoints(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("答案要点数据异常", exception);
        }
    }

    public String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("数据序列化失败", exception);
        }
    }

    public String timeState(Activity activity) {
        Instant now = Instant.now();
        if (activity.getStatus() == ActivityStatus.DRAFT) return "DRAFT";
        if (activity.getStatus() == ActivityStatus.ARCHIVED) return "ARCHIVED";
        if (activity.getStatus() == ActivityStatus.CLOSED || !now.isBefore(activity.getEndAt())) return "ENDED";
        if (now.isBefore(activity.getStartAt())) return "UPCOMING";
        return "ACTIVE";
    }

    private void validateTime(ActivityDtos.UpsertActivityRequest request) {
        DeadlineMode mode = request.deadlineMode() == null ? DeadlineMode.FIXED_TIME : request.deadlineMode();
        if (mode == DeadlineMode.DURATION) requireDurationHours(request.durationHours());
        if (!request.endAt().isAfter(request.startAt())) {
            throw BusinessException.badRequest("ACTIVITY_TIME_ERROR", "截止时间必须晚于开始时间");
        }
    }

    private int requireDurationHours(Integer durationHours) {
        if (durationHours == null || durationHours < 1 || durationHours > 720) {
            throw BusinessException.badRequest("ACTIVITY_DURATION_ERROR", "答题时长必须在1至720小时之间");
        }
        return durationHours;
    }

    private void validateReleaseContent(Activity activity) {
        if (!RELEASE_CATEGORIES.contains(activity.getCategory())) {
            throw BusinessException.badRequest("PUBLISH_CONTENT_RESTRICTED",
                    "首个正式版未完成宗教信息服务资质确认，当前分类不能发布");
        }
        String content = String.join(" ",
                value(activity.getTitle()), value(activity.getSubtitle()), value(activity.getQuestionText()),
                value(activity.getSourceTitle()), value(activity.getSourceDetail()), value(activity.getStandardAnswer()));
        if (RELEASE_RESTRICTED_TERMS.stream().anyMatch(content::contains)) {
            throw BusinessException.badRequest("PUBLISH_CONTENT_RESTRICTED",
                    "题目包含首版暂不上线的宗教或师门讲解表述，请按传统文化与经典哲思口径调整后再发布");
        }
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private void apply(Activity activity, ActivityDtos.UpsertActivityRequest request) {
        activity.setTitle(request.title().trim());
        activity.setSubtitle(trimToNull(request.subtitle()));
        activity.setCategory(request.category());
        activity.setQuestionText(request.questionText().trim());
        activity.setSourceTitle(trimToNull(request.sourceTitle()));
        activity.setSourceDetail(trimToNull(request.sourceDetail()));
        activity.setStandardAnswer(request.standardAnswer().trim());
        activity.setAnswerPoints(toJson(request.answerPoints().stream().map(String::trim).toList()));
        activity.setRunnerUpThreshold(request.runnerUpThreshold());
        DeadlineMode mode = request.deadlineMode() == null ? DeadlineMode.FIXED_TIME : request.deadlineMode();
        activity.setDeadlineMode(mode);
        activity.setDurationHours(mode == DeadlineMode.DURATION ? requireDurationHours(request.durationHours()) : null);
        activity.setStartAt(request.startAt());
        activity.setEndAt(request.endAt());
        activity.setRevealMode(request.revealMode());
        activity.setMaxAttempts(request.maxAttempts());
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ActivityDtos.AdminActivitySummary toAdminSummary(Activity activity) {
        return new ActivityDtos.AdminActivitySummary(
                activity.getId(), activity.getTitle(), activity.getSubtitle(), activity.getCategory(),
                activity.getStatus(), timeState(activity), activity.getStartAt(), activity.getEndAt(),
                submissionRepository.countByActivityId(activity.getId())
                        + snapshotRepository.countByActivityId(activity.getId()),
                submissionRepository.countByActivityIdAndGrade(activity.getId(), SubmissionGrade.CHAMPION),
                submissionRepository.countByActivityIdAndGrade(activity.getId(), SubmissionGrade.RUNNER_UP),
                activity.getUpdatedAt());
    }

    private ActivityDtos.AdminActivityDetail toAdminDetail(Activity activity) {
        return new ActivityDtos.AdminActivityDetail(
                activity.getId(), activity.getTitle(), activity.getSubtitle(), activity.getCategory(),
                activity.getQuestionText(), activity.getSourceTitle(), activity.getSourceDetail(),
                activity.getStandardAnswer(), parsePoints(activity.getAnswerPoints()), activity.getRunnerUpThreshold(),
                activity.getDeadlineMode(), activity.getDurationHours(),
                activity.getStartAt(), activity.getEndAt(), activity.getStatus(), activity.getRevealMode(),
                activity.getMaxAttempts(), activity.getPublishedAt(), activity.getClosedAt(), activity.getVersion());
    }
}
