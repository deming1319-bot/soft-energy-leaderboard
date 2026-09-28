package com.softenergy.leaderboard.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "activities")
public class Activity {
    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 240)
    private String subtitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CultureCategory category;

    @Lob
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "source_title", length = 160)
    private String sourceTitle;

    @Column(name = "source_detail", length = 500)
    private String sourceDetail;

    @Lob
    @Column(name = "standard_answer", nullable = false, columnDefinition = "TEXT")
    private String standardAnswer;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer_points", nullable = false, columnDefinition = "JSON")
    private String answerPoints = "[]";

    @Column(name = "runner_up_threshold", nullable = false, precision = 5, scale = 4)
    private BigDecimal runnerUpThreshold = new BigDecimal("0.6000");

    @Enumerated(EnumType.STRING)
    @Column(name = "deadline_mode", nullable = false, length = 24)
    private DeadlineMode deadlineMode = DeadlineMode.FIXED_TIME;

    @Column(name = "duration_hours")
    private Integer durationHours;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ActivityStatus status = ActivityStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "reveal_mode", nullable = false, length = 32)
    private RevealMode revealMode = RevealMode.AFTER_DEADLINE;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 1;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;
}
