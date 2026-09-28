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
@Table(name = "submissions")
public class Submission {
    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private MiniappUser user;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Column(name = "client_request_id", nullable = false, length = 64)
    private String clientRequestId;

    @Lob
    @Column(name = "answer_text", nullable = false, columnDefinition = "TEXT")
    private String answerText;

    @Lob
    @Column(name = "normalized_answer", nullable = false, columnDefinition = "TEXT")
    private String normalizedAnswer;

    @Enumerated(EnumType.STRING)
    @Column(name = "original_grade", nullable = false, length = 24)
    private SubmissionGrade originalGrade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SubmissionGrade grade;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal score;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matched_points", nullable = false, columnDefinition = "JSON")
    private String matchedPoints = "[]";

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by", length = 64)
    private String reviewedBy;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
