package com.softenergy.leaderboard.domain.repository;

import com.softenergy.leaderboard.domain.model.LeaderboardSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaderboardSnapshotRepository extends JpaRepository<LeaderboardSnapshot, String> {
    List<LeaderboardSnapshot> findByActivityIdOrderBySubmittedAtAsc(String activityId);

    long countByActivityId(String activityId);
}
