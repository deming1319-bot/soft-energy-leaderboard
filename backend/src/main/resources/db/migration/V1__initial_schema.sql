CREATE TABLE admin_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP(3) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE miniapp_users (
    id VARCHAR(36) NOT NULL,
    openid VARCHAR(128) NOT NULL,
    unionid VARCHAR(128) NULL,
    nickname VARCHAR(64) NOT NULL,
    avatar_url VARCHAR(512) NULL,
    phone_encrypted TEXT NULL,
    phone_hash VARCHAR(64) NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP(3) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_miniapp_users_openid (openid),
    UNIQUE KEY uk_miniapp_users_phone_hash (phone_hash),
    KEY idx_miniapp_users_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE activities (
    id VARCHAR(36) NOT NULL,
    title VARCHAR(120) NOT NULL,
    subtitle VARCHAR(240) NULL,
    category VARCHAR(32) NOT NULL,
    question_text TEXT NOT NULL,
    source_title VARCHAR(160) NULL,
    source_detail VARCHAR(500) NULL,
    standard_answer TEXT NOT NULL,
    answer_points JSON NOT NULL,
    runner_up_threshold DECIMAL(5,4) NOT NULL DEFAULT 0.6000,
    start_at TIMESTAMP(3) NOT NULL,
    end_at TIMESTAMP(3) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    reveal_mode VARCHAR(32) NOT NULL DEFAULT 'AFTER_DEADLINE',
    max_attempts INT NOT NULL DEFAULT 1,
    published_at TIMESTAMP(3) NULL,
    closed_at TIMESTAMP(3) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_activities_status_time (status, start_at, end_at),
    CONSTRAINT ck_activities_threshold CHECK (runner_up_threshold >= 0 AND runner_up_threshold <= 1),
    CONSTRAINT ck_activities_attempts CHECK (max_attempts >= 1),
    CONSTRAINT ck_activities_time CHECK (end_at > start_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE submissions (
    id VARCHAR(36) NOT NULL,
    activity_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    attempt_no INT NOT NULL,
    client_request_id VARCHAR(64) NOT NULL,
    answer_text TEXT NOT NULL,
    normalized_answer TEXT NOT NULL,
    original_grade VARCHAR(24) NOT NULL,
    grade VARCHAR(24) NOT NULL,
    score DECIMAL(5,4) NOT NULL,
    matched_points JSON NOT NULL,
    submitted_at TIMESTAMP(3) NOT NULL,
    reviewed_at TIMESTAMP(3) NULL,
    reviewed_by VARCHAR(64) NULL,
    review_note VARCHAR(500) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_submissions_activity_user_attempt (activity_id, user_id, attempt_no),
    UNIQUE KEY uk_submissions_activity_user_request (activity_id, user_id, client_request_id),
    KEY idx_submissions_activity_grade_time (activity_id, grade, submitted_at),
    KEY idx_submissions_user_time (user_id, submitted_at),
    CONSTRAINT fk_submissions_activity FOREIGN KEY (activity_id) REFERENCES activities (id),
    CONSTRAINT fk_submissions_user FOREIGN KEY (user_id) REFERENCES miniapp_users (id),
    CONSTRAINT ck_submissions_score CHECK (score >= 0 AND score <= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE admin_audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    admin_username VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(64) NULL,
    summary VARCHAR(500) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_admin_audit_logs_created_at (created_at),
    KEY idx_admin_audit_logs_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
