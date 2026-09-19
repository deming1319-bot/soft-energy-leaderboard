package com.softenergy.leaderboard.domain.repository;

import com.softenergy.leaderboard.domain.model.Submission;
import com.softenergy.leaderboard.domain.model.SubmissionGrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, String> {
    List<Submission> findByActivityIdOrderBySubmittedAtAsc(String activityId);

    List<Submission> findByActivityIdAndGradeInOrderBySubmittedAtAsc(
            String activityId, Collection<SubmissionGrade> grades);

    List<Submission> findByUserIdOrderBySubmittedAtDesc(String userId);

    Optional<Submission> findFirstByActivityIdAndUserIdOrderByAttemptNoDesc(String activityId, String userId);

    Optional<Submission> findByActivityIdAndUserIdAndClientRequestId(
            String activityId, String userId, String clientRequestId);

    long countByActivityId(String activityId);

    long countByActivityIdAndGrade(String activityId, SubmissionGrade grade);

    long countByGrade(SubmissionGrade grade);

    long countByUserId(String userId);

    long countByUserIdAndGrade(String userId, SubmissionGrade grade);
}

