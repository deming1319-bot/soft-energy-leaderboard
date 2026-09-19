package com.softenergy.leaderboard.domain.repository;

import com.softenergy.leaderboard.domain.model.ContentReport;
import com.softenergy.leaderboard.domain.model.ReportStatus;
import com.softenergy.leaderboard.domain.model.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ContentReportRepository extends JpaRepository<ContentReport, String> {
    List<ContentReport> findAllByOrderByCreatedAtDesc();

    List<ContentReport> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    List<ContentReport> findByReporterIdOrderByCreatedAtDesc(String reporterId);

    long countByReporterIdAndStatusIn(String reporterId, Collection<ReportStatus> statuses);

    void deleteByReporterId(String reporterId);

    void deleteByTargetTypeAndTargetId(ReportTargetType targetType, String targetId);
}
