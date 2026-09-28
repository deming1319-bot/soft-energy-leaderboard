package com.softenergy.leaderboard.service;

import com.softenergy.leaderboard.api.dto.UserDtos;
import com.softenergy.leaderboard.domain.model.MiniappUser;
import com.softenergy.leaderboard.domain.model.LeaderboardSnapshot;
import com.softenergy.leaderboard.domain.model.ReportTargetType;
import com.softenergy.leaderboard.domain.model.Submission;
import com.softenergy.leaderboard.domain.model.SubmissionGrade;
import com.softenergy.leaderboard.domain.model.UserStatus;
import com.softenergy.leaderboard.domain.repository.ContentReportRepository;
import com.softenergy.leaderboard.domain.repository.LeaderboardSnapshotRepository;
import com.softenergy.leaderboard.domain.repository.MiniappUserRepository;
import com.softenergy.leaderboard.domain.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {
    private final MiniappUserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final PhoneCryptoService phoneCryptoService;
    private final SubmissionService submissionService;
    private final WechatContentSecurityService contentSecurityService;
    private final LeaderboardSnapshotRepository snapshotRepository;
    private final ContentReportRepository reportRepository;
    private final AuditService auditService;

    public UserService(
            MiniappUserRepository userRepository,
            SubmissionRepository submissionRepository,
            PhoneCryptoService phoneCryptoService,
            SubmissionService submissionService,
            WechatContentSecurityService contentSecurityService,
            LeaderboardSnapshotRepository snapshotRepository,
            ContentReportRepository reportRepository,
            AuditService auditService) {
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
        this.phoneCryptoService = phoneCryptoService;
        this.submissionService = submissionService;
        this.contentSecurityService = contentSecurityService;
        this.snapshotRepository = snapshotRepository;
        this.reportRepository = reportRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<UserDtos.UserListItem> list() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(MiniappUser::getCreatedAt).reversed())
                .map(user -> new UserDtos.UserListItem(
                        user.getId(), user.getNickname(), user.getAvatarUrl(),
                        phoneCryptoService.maskEncrypted(user.getPhoneEncrypted()), user.getStatus().name(),
                        submissionRepository.countByUserId(user.getId()),
                        submissionRepository.countByUserIdAndGrade(user.getId(), SubmissionGrade.CHAMPION),
                        submissionRepository.countByUserIdAndGrade(user.getId(), SubmissionGrade.RUNNER_UP),
                        user.getLastLoginAt(), user.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDtos.MiniappProfile profile(String userId) {
        MiniappUser user = requireActive(userId);
        var recent = submissionRepository.findByUserIdOrderBySubmittedAtDesc(userId).stream()
                .limit(10)
                .map(item -> submissionService.toResult(item, false))
                .toList();
        return new UserDtos.MiniappProfile(
                user.getId(), user.getNickname(), user.getAvatarUrl(),
                phoneCryptoService.maskEncrypted(user.getPhoneEncrypted()),
                profileComplete(user), user.getPrivacyVersion(), user.getTermsVersion(), user.getConsentedAt(),
                submissionRepository.countByUserId(userId),
                submissionRepository.countByUserIdAndGrade(userId, SubmissionGrade.CHAMPION),
                submissionRepository.countByUserIdAndGrade(userId, SubmissionGrade.RUNNER_UP), recent);
    }

    @Transactional
    public UserDtos.MiniappProfile updateProfile(
            String userId, UserDtos.UpdateMiniappProfileRequest request) {
        MiniappUser user = requireActive(userId);
        String nickname = request.nickname().trim();
        contentSecurityService.checkText(user.getOpenid(), nickname);
        user.setNickname(nickname);
        return profile(userId);
    }

    @Transactional
    public void cancelAccount(String userId) {
        MiniappUser user = requireActive(userId);
        List<Submission> submissions = submissionRepository.findByUserIdOrderBySubmittedAtDesc(userId);
        Map<String, Submission> bestByActivity = new LinkedHashMap<>();
        for (Submission submission : submissions) {
            if (submission.getGrade() != SubmissionGrade.CHAMPION
                    && submission.getGrade() != SubmissionGrade.RUNNER_UP) continue;
            bestByActivity.merge(submission.getActivity().getId(), submission, this::betterSubmission);
        }
        for (Submission submission : bestByActivity.values()) {
            LeaderboardSnapshot snapshot = new LeaderboardSnapshot();
            snapshot.setActivity(submission.getActivity());
            snapshot.setDisplayName("已注销用户");
            snapshot.setGrade(submission.getGrade());
            snapshot.setSubmittedAt(submission.getSubmittedAt());
            snapshotRepository.save(snapshot);
        }

        reportRepository.deleteByReporterId(userId);
        reportRepository.deleteByTargetTypeAndTargetId(ReportTargetType.USER, userId);
        submissionRepository.deleteAll(submissions);
        submissionRepository.flush();
        userRepository.delete(user);
        auditService.record("system", "ACCOUNT_ERASURE", "USER", null,
                "用户完成自主注销，身份、原始答案及可关联记录已清除");
    }

    private Submission betterSubmission(Submission left, Submission right) {
        if (left.getGrade() != right.getGrade()) {
            return left.getGrade() == SubmissionGrade.CHAMPION ? left : right;
        }
        return left.getSubmittedAt().isBefore(right.getSubmittedAt()) ? left : right;
    }

    private MiniappUser requireActive(String userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> com.softenergy.leaderboard.api.BusinessException.notFound("用户不存在或已注销"));
    }

    private boolean profileComplete(MiniappUser user) {
        return user.getNickname() != null && !user.getNickname().isBlank()
                && !"修习者".equals(user.getNickname());
    }
}
