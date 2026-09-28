package com.softenergy.leaderboard.service;

import com.softenergy.leaderboard.api.BusinessException;
import com.softenergy.leaderboard.api.dto.SubmissionDtos;
import com.softenergy.leaderboard.domain.model.*;
import com.softenergy.leaderboard.domain.repository.MiniappUserRepository;
import com.softenergy.leaderboard.domain.repository.LeaderboardSnapshotRepository;
import com.softenergy.leaderboard.domain.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class SubmissionService {
    private final SubmissionRepository repository;
    private final MiniappUserRepository userRepository;
    private final ActivityService activityService;
    private final JudgmentService judgmentService;
    private final PhoneCryptoService phoneCryptoService;
    private final AuditService auditService;
    private final WechatContentSecurityService contentSecurityService;
    private final LeaderboardSnapshotRepository snapshotRepository;

    public SubmissionService(
            SubmissionRepository repository,
            MiniappUserRepository userRepository,
            ActivityService activityService,
            JudgmentService judgmentService,
            PhoneCryptoService phoneCryptoService,
            AuditService auditService,
            WechatContentSecurityService contentSecurityService,
            LeaderboardSnapshotRepository snapshotRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.activityService = activityService;
        this.judgmentService = judgmentService;
        this.phoneCryptoService = phoneCryptoService;
        this.auditService = auditService;
        this.contentSecurityService = contentSecurityService;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional
    public SubmissionDtos.SubmissionResult submit(
            String activityId, String userId, SubmissionDtos.SubmitAnswerRequest request) {
        Optional<Submission> idempotent = repository.findByActivityIdAndUserIdAndClientRequestId(
                activityId, userId, request.clientRequestId());
        if (idempotent.isPresent()) return toResult(idempotent.get(), false);

        Activity activity = activityService.requirePublic(activityId);
        String timeState = activityService.timeState(activity);
        if ("UPCOMING".equals(timeState)) {
            throw BusinessException.conflict("ACTIVITY_NOT_STARTED", "活动尚未开始");
        }
        if (!"ACTIVE".equals(timeState)) {
            throw BusinessException.conflict("ACTIVITY_ENDED", "活动已经截止");
        }
        MiniappUser user = requireUser(userId);
        contentSecurityService.checkText(user.getOpenid(), request.answer());
        List<Submission> userSubmissions = repository.findByActivityIdOrderBySubmittedAtAsc(activityId).stream()
                .filter(item -> item.getUser().getId().equals(userId))
                .toList();
        if (userSubmissions.size() >= activity.getMaxAttempts()) {
            throw BusinessException.conflict("ATTEMPT_LIMIT", "本场答题次数已用完");
        }

        List<String> answerPoints = activityService.parsePoints(activity.getAnswerPoints());
        JudgmentService.JudgmentResult judgment = judgmentService.judge(
                request.answer(), activity.getStandardAnswer(), answerPoints, activity.getRunnerUpThreshold());
        Submission submission = new Submission();
        submission.setActivity(activity);
        submission.setUser(user);
        submission.setAttemptNo(userSubmissions.size() + 1);
        submission.setClientRequestId(request.clientRequestId());
        submission.setAnswerText(request.answer().trim());
        submission.setNormalizedAnswer(judgment.normalizedAnswer());
        submission.setOriginalGrade(judgment.grade());
        submission.setGrade(judgment.grade());
        submission.setScore(judgment.score());
        submission.setMatchedPoints(activityService.toJson(judgment.matchedPoints()));
        submission.setSubmittedAt(Instant.now());
        return toResult(repository.save(submission), true);
    }

    @Transactional(readOnly = true)
    public SubmissionDtos.SubmissionResult myLatest(String activityId, String userId) {
        Submission submission = repository.findFirstByActivityIdAndUserIdOrderByAttemptNoDesc(activityId, userId)
                .orElseThrow(() -> BusinessException.notFound("尚未提交答案"));
        return toResult(submission, false);
    }

    @Transactional(readOnly = true)
    public List<SubmissionDtos.AdminSubmissionItem> adminList(String activityId) {
        activityService.require(activityId);
        return repository.findByActivityIdOrderBySubmittedAtAsc(activityId).stream()
                .map(this::toAdminItem)
                .toList();
    }

    @Transactional
    public SubmissionDtos.AdminSubmissionItem review(
            String submissionId, SubmissionDtos.ReviewSubmissionRequest request, String admin) {
        if (request.grade() == SubmissionGrade.REVIEW_REQUIRED) {
            throw BusinessException.badRequest("REVIEW_GRADE_ERROR", "人工复核必须给出明确结果");
        }
        Submission submission = repository.findById(submissionId)
                .orElseThrow(() -> BusinessException.notFound("答题记录不存在"));
        submission.setGrade(request.grade());
        submission.setReviewNote(request.note().trim());
        submission.setReviewedBy(admin);
        submission.setReviewedAt(Instant.now());
        auditService.record(admin, "REVIEW_SUBMISSION", "SUBMISSION", submissionId,
                "将“" + submission.getUser().getNickname() + "”的判定调整为 " + request.grade());
        return toAdminItem(submission);
    }

    @Transactional(readOnly = true)
    public SubmissionDtos.LeaderboardResponse leaderboard(String activityId, String currentUserId, boolean adminView) {
        Activity activity = adminView ? activityService.require(activityId) : activityService.requirePublic(activityId);
        boolean revealed = adminView || "ENDED".equals(activityService.timeState(activity));
        List<Submission> all = repository.findByActivityIdOrderBySubmittedAtAsc(activityId);
        SubmissionDtos.SubmissionResult myResult = null;
        if (currentUserId != null) {
            myResult = all.stream()
                    .filter(item -> item.getUser().getId().equals(currentUserId))
                    .max(Comparator.comparingInt(Submission::getAttemptNo))
                    .map(item -> toResult(item, false))
                    .orElse(null);
        }
        if (!revealed) {
            return new SubmissionDtos.LeaderboardResponse(
                    activity.getId(), activity.getTitle(), false, activity.getEndAt(), List.of(), List.of(), myResult);
        }

        Map<String, Submission> bestByUser = new LinkedHashMap<>();
        for (Submission submission : all) {
            if (submission.getGrade() != SubmissionGrade.CHAMPION
                    && submission.getGrade() != SubmissionGrade.RUNNER_UP) continue;
            bestByUser.merge(submission.getUser().getId(), submission, this::betterSubmission);
        }
        List<RankingCandidate> candidates = new ArrayList<>(bestByUser.values().stream()
                .map(item -> new RankingCandidate(
                        item.getId(), item.getUser().getId(), item.getUser().getNickname(),
                        item.getUser().getAvatarUrl(), item.getGrade(), item.getSubmittedAt(),
                        Objects.equals(currentUserId, item.getUser().getId())))
                .toList());
        snapshotRepository.findByActivityIdOrderBySubmittedAtAsc(activityId).forEach(snapshot ->
                candidates.add(new RankingCandidate(
                        snapshot.getId(), "anonymous:" + snapshot.getId(), snapshot.getDisplayName(), null,
                        snapshot.getGrade(), snapshot.getSubmittedAt(), false)));
        Comparator<RankingCandidate> rankingOrder = Comparator
                .comparing(RankingCandidate::submittedAt).thenComparing(RankingCandidate::stableId);
        List<RankingCandidate> champions = candidates.stream()
                .filter(item -> item.grade() == SubmissionGrade.CHAMPION)
                .sorted(rankingOrder)
                .toList();
        List<RankingCandidate> runners = candidates.stream()
                .filter(item -> item.grade() == SubmissionGrade.RUNNER_UP)
                .sorted(rankingOrder)
                .toList();
        return new SubmissionDtos.LeaderboardResponse(
                activity.getId(), activity.getTitle(), true, activity.getEndAt(),
                toEntries(champions, currentUserId), toEntries(runners, currentUserId), myResult);
    }

    public SubmissionDtos.SubmissionResult toResult(Submission submission, boolean justSubmitted) {
        Activity activity = submission.getActivity();
        boolean reveal = activity.getRevealMode() == RevealMode.AFTER_SUBMIT
                || "ENDED".equals(activityService.timeState(activity));
        return new SubmissionDtos.SubmissionResult(
                submission.getId(), activity.getId(), activity.getTitle(), submission.getGrade(),
                submission.getScore(), activityService.parsePoints(submission.getMatchedPoints()),
                submission.getSubmittedAt(), feedback(submission.getGrade(), justSubmitted),
                reveal, reveal ? activity.getStandardAnswer() : null, submission.getReviewNote());
    }

    public SubmissionDtos.AdminSubmissionItem toAdminItem(Submission submission) {
        return new SubmissionDtos.AdminSubmissionItem(
                submission.getId(), submission.getUser().getId(), submission.getUser().getNickname(),
                phoneCryptoService.maskEncrypted(submission.getUser().getPhoneEncrypted()), submission.getAttemptNo(),
                submission.getAnswerText(), submission.getOriginalGrade(), submission.getGrade(),
                submission.getScore(), activityService.parsePoints(submission.getMatchedPoints()),
                submission.getSubmittedAt(), submission.getReviewedBy(), submission.getReviewedAt(),
                submission.getReviewNote());
    }

    private Submission betterSubmission(Submission left, Submission right) {
        int leftPriority = gradePriority(left.getGrade());
        int rightPriority = gradePriority(right.getGrade());
        if (leftPriority != rightPriority) return leftPriority < rightPriority ? left : right;
        return left.getSubmittedAt().isBefore(right.getSubmittedAt()) ? left : right;
    }

    private int gradePriority(SubmissionGrade grade) {
        return grade == SubmissionGrade.CHAMPION ? 0 : 1;
    }

    private List<SubmissionDtos.LeaderboardEntry> toEntries(
            List<RankingCandidate> submissions, String currentUserId) {
        List<SubmissionDtos.LeaderboardEntry> result = new ArrayList<>();
        for (int index = 0; index < submissions.size(); index++) {
            RankingCandidate item = submissions.get(index);
            result.add(new SubmissionDtos.LeaderboardEntry(
                    index + 1, item.userId(), item.displayName(), item.avatarUrl(),
                    item.grade(), item.submittedAt(), item.currentUser()));
        }
        return result;
    }

    private String feedback(SubmissionGrade grade, boolean justSubmitted) {
        return switch (grade) {
            case CHAMPION -> "回答完整且顺序准确，已进入冠军榜。";
            case RUNNER_UP -> "两句内容完全正确，但上下联顺序颠倒，已进入亚军榜。";
            case REVIEW_REQUIRED -> "答案接近入榜标准，已提交老师复核。";
            case UNRANKED -> justSubmitted
                    ? "本次答案暂未达到入榜标准，记录已经妥善保存。"
                    : "本次答案未进入公开榜单。";
        };
    }

    private MiniappUser requireUser(String id) {
        return userRepository.findById(id).filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.notFound("用户不存在或已停用"));
    }

    private record RankingCandidate(
            String stableId,
            String userId,
            String displayName,
            String avatarUrl,
            SubmissionGrade grade,
            Instant submittedAt,
            boolean currentUser) {}
}
