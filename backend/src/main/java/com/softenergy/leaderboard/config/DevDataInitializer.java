package com.softenergy.leaderboard.config;

import com.softenergy.leaderboard.domain.model.*;
import com.softenergy.leaderboard.domain.repository.*;
import com.softenergy.leaderboard.service.ActivityService;
import com.softenergy.leaderboard.service.JudgmentService;
import com.softenergy.leaderboard.service.PhoneCryptoService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Profile("dev")
@Component
public class DevDataInitializer implements ApplicationRunner {
    private final SoftEnergyProperties properties;
    private final AdminUserRepository adminUserRepository;
    private final MiniappUserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final SubmissionRepository submissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityService activityService;
    private final JudgmentService judgmentService;
    private final PhoneCryptoService phoneCryptoService;

    public DevDataInitializer(
            SoftEnergyProperties properties,
            AdminUserRepository adminUserRepository,
            MiniappUserRepository userRepository,
            ActivityRepository activityRepository,
            SubmissionRepository submissionRepository,
            PasswordEncoder passwordEncoder,
            ActivityService activityService,
            JudgmentService judgmentService,
            PhoneCryptoService phoneCryptoService) {
        this.properties = properties;
        this.adminUserRepository = adminUserRepository;
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.submissionRepository = submissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.activityService = activityService;
        this.judgmentService = judgmentService;
        this.phoneCryptoService = phoneCryptoService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        bootstrapAdmin();
        if (properties.getBootstrap().isDemoDataEnabled() && activityRepository.count() == 0) {
            bootstrapDemoData();
        }
    }

    private void bootstrapAdmin() {
        String username = properties.getBootstrap().getAdminUsername().trim();
        AdminUser admin = adminUserRepository.findByUsername(username)
                .orElseGet(() -> {
                    List<AdminUser> existingAdmins = adminUserRepository.findAll();
                    return existingAdmins.size() == 1 ? existingAdmins.getFirst() : new AdminUser();
                });
        admin.setUsername(username);
        admin.setDisplayName(properties.getBootstrap().getAdminDisplayName());
        admin.setPasswordHash(passwordEncoder.encode(properties.getBootstrap().getAdminPassword()));
        admin.setEnabled(true);
        adminUserRepository.save(admin);
    }

    private void bootstrapDemoData() {
        Instant now = Instant.now();
        List<String> activePoints = List.of("知止而后有定，定而后能静", "静而后能安，安而后能虑");
        Activity active = new Activity();
        active.setTitle("今日修习 · 知止而后有定");
        active.setSubtitle("从《大学》的次第中，体会安顿身心的方法");
        active.setCategory(CultureCategory.CLASSICS);
        active.setQuestionText("请严格按照上联、下联的顺序，逐字写出题目要求的两句内容。");
        active.setSourceTitle("《礼记·大学》");
        active.setSourceDetail("经典原文与内容编辑组整理，当前为演示内容。");
        active.setStandardAnswer(String.join("\n", activePoints));
        active.setAnswerPoints(activityService.toJson(activePoints));
        active.setRunnerUpThreshold(new BigDecimal("0.9000"));
        active.setDeadlineMode(DeadlineMode.FIXED_TIME);
        active.setStartAt(now.minus(1, ChronoUnit.HOURS));
        active.setEndAt(now.plus(10, ChronoUnit.HOURS));
        active.setStatus(ActivityStatus.PUBLISHED);
        active.setPublishedAt(now.minus(1, ChronoUnit.HOURS));
        active.setRevealMode(RevealMode.AFTER_DEADLINE);
        active.setMaxAttempts(1);
        activityRepository.save(active);

        List<String> endedPoints = List.of("上善若水，水善利万物而不争", "处众人之所恶，故几于道");
        Activity ended = new Activity();
        ended.setTitle("往期回顾 · 上善若水");
        ended.setSubtitle("在不争之中理解柔韧与成全");
        ended.setCategory(CultureCategory.PHILOSOPHY);
        ended.setQuestionText("请按原有次序，逐字写出“上善若水”一章的两句内容。");
        ended.setSourceTitle("《道德经》第八章");
        ended.setSourceDetail("经典原文与内容编辑组整理，当前为演示内容。");
        ended.setStandardAnswer(String.join("\n", endedPoints));
        ended.setAnswerPoints(activityService.toJson(endedPoints));
        ended.setRunnerUpThreshold(new BigDecimal("0.9000"));
        ended.setDeadlineMode(DeadlineMode.FIXED_TIME);
        ended.setStartAt(now.minus(14, ChronoUnit.HOURS));
        ended.setEndAt(now.minus(2, ChronoUnit.HOURS));
        ended.setStatus(ActivityStatus.CLOSED);
        ended.setPublishedAt(now.minus(14, ChronoUnit.HOURS));
        ended.setClosedAt(now.minus(2, ChronoUnit.HOURS));
        ended.setRevealMode(RevealMode.AFTER_DEADLINE);
        activityRepository.save(ended);

        Activity draft = new Activity();
        draft.setTitle("明日预备 · 一念清明");
        draft.setSubtitle("从观照中辨认真实的起心动念");
        draft.setCategory(CultureCategory.LIFE_PRACTICE);
        draft.setQuestionText("请逐字写出生活实践笔记中的两句提醒。");
        draft.setSourceTitle("生活实践笔记");
        draft.setSourceDetail("原创内容示例，发布前需由内容负责人复核。");
        draft.setStandardAnswer("先觉察念头，再辨认动机\n保持清楚观照，再选择合宜行动");
        draft.setAnswerPoints(activityService.toJson(List.of("先觉察念头，再辨认动机", "保持清楚观照，再选择合宜行动")));
        draft.setRunnerUpThreshold(new BigDecimal("0.9000"));
        draft.setDeadlineMode(DeadlineMode.DURATION);
        draft.setDurationHours(12);
        draft.setStartAt(now);
        draft.setEndAt(now.plus(12, ChronoUnit.HOURS));
        draft.setStatus(ActivityStatus.DRAFT);
        activityRepository.save(draft);

        MiniappUser user1 = createUser("demo-openid-1", "林知远", "00000000001");
        MiniappUser user2 = createUser("demo-openid-2", "周静安", "00000000002");
        MiniappUser user3 = createUser("demo-openid-3", "陈明澈", "00000000003");
        MiniappUser user4 = createUser("demo-openid-4", "许若水", "00000000004");

        createSubmission(ended, user1, ended.getStandardAnswer(), now.minus(12, ChronoUnit.HOURS));
        createSubmission(ended, user2, String.join("\n", endedPoints.reversed()), now.minus(11, ChronoUnit.HOURS));
        createSubmission(ended, user3, ended.getStandardAnswer() + "😊", now.minus(10, ChronoUnit.HOURS));
        createSubmission(ended, user4, "水能够帮助万物，不争强好胜，所以接近于道。", now.minus(9, ChronoUnit.HOURS));
    }

    private MiniappUser createUser(String openid, String nickname, String phone) {
        MiniappUser user = new MiniappUser();
        user.setOpenid(openid);
        user.setNickname(nickname);
        user.setPhoneEncrypted(phoneCryptoService.encrypt(phone));
        user.setPhoneHash(phoneCryptoService.hash(phone));
        user.setLastLoginAt(Instant.now().minus(1, ChronoUnit.DAYS));
        return userRepository.save(user);
    }

    private void createSubmission(Activity activity, MiniappUser user, String answer, Instant submittedAt) {
        var result = judgmentService.judge(
                answer, activity.getStandardAnswer(), activityService.parsePoints(activity.getAnswerPoints()),
                activity.getRunnerUpThreshold());
        Submission submission = new Submission();
        submission.setActivity(activity);
        submission.setUser(user);
        submission.setAttemptNo(1);
        submission.setClientRequestId("seed-" + user.getId());
        submission.setAnswerText(answer);
        submission.setNormalizedAnswer(result.normalizedAnswer());
        submission.setOriginalGrade(result.grade());
        submission.setGrade(result.grade());
        submission.setScore(result.score());
        submission.setMatchedPoints(activityService.toJson(result.matchedPoints()));
        submission.setSubmittedAt(submittedAt);
        submissionRepository.save(submission);
    }
}
