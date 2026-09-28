package com.softenergy.leaderboard.domain.repository;

import com.softenergy.leaderboard.domain.model.Activity;
import com.softenergy.leaderboard.domain.model.ActivityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, String> {
    List<Activity> findByStatusInOrderByStartAtDesc(Collection<ActivityStatus> statuses);

    List<Activity> findByStatusAndEndAtBefore(ActivityStatus status, Instant now);

    long countByStatus(ActivityStatus status);
}

