CREATE TABLE leaderboard_snapshots (
    id VARCHAR(36) NOT NULL,
    activity_id VARCHAR(36) NOT NULL,
    display_name VARCHAR(64) NOT NULL DEFAULT '已注销用户',
    grade VARCHAR(24) NOT NULL,
    submitted_at TIMESTAMP(3) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_leaderboard_snapshots_activity_grade_time (activity_id, grade, submitted_at),
    CONSTRAINT fk_leaderboard_snapshots_activity FOREIGN KEY (activity_id) REFERENCES activities (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE content_reports (
    id VARCHAR(36) NOT NULL,
    reporter_user_id VARCHAR(36) NULL,
    type VARCHAR(40) NOT NULL,
    target_type VARCHAR(24) NOT NULL,
    target_id VARCHAR(64) NULL,
    target_label VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    handling_note VARCHAR(500) NULL,
    handled_by VARCHAR(64) NULL,
    handled_at TIMESTAMP(3) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_content_reports_status_created (status, created_at),
    KEY idx_content_reports_reporter_created (reporter_user_id, created_at),
    KEY idx_content_reports_target (target_type, target_id),
    CONSTRAINT fk_content_reports_reporter FOREIGN KEY (reporter_user_id)
        REFERENCES miniapp_users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
