package com.softenergy.leaderboard.service;

import com.softenergy.leaderboard.api.dto.DashboardDtos;
import com.softenergy.leaderboard.domain.model.ActivityStatus;
import com.softenergy.leaderboard.domain.model.SubmissionGrade;
import com.softenergy.leaderboard.domain.repository.ActivityRepository;
import com.softenergy.leaderboard.domain.repository.MiniappUserRepository;
import com.softenergy.leaderboard.domain.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

@Service
public class DashboardService {
    private final MiniappUserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final SubmissionRepository submissionRepository;

    public DashboardService(
            MiniappUserRepository userRepository,
            ActivityRepository activityRepository,
            SubmissionRepository submissionRepository) {
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
        this.submissionRepository = submissionRepository;
    }

    @Transactional(readOnly = true)
    public DashboardDtos.DashboardOverview overview() {
        var recent = submissionRepository.findAll().stream()
                .sorted(Comparator.comparing(com.softenergy.leaderboard.domain.model.Submission::getSubmittedAt).reversed())
                .limit(8)
                .map(item -> new DashboardDtos.RecentSubmission(
                        item.getId(), item.getActivity().getTitle(), item.getUser().getNickname(),
                        item.getGrade().name(), item.getSubmittedAt()))
                .toList();
        return new DashboardDtos.DashboardOverview(
                userRepository.count(), activityRepository.countByStatus(ActivityStatus.PUBLISHED),
                submissionRepository.count(), submissionRepository.countByGrade(SubmissionGrade.CHAMPION),
                submissionRepository.countByGrade(SubmissionGrade.RUNNER_UP),
                submissionRepository.countByGrade(SubmissionGrade.REVIEW_REQUIRED), recent);
    }
}

