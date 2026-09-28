ALTER TABLE activities
    ADD COLUMN deadline_mode VARCHAR(24) NOT NULL DEFAULT 'FIXED_TIME' AFTER runner_up_threshold,
    ADD COLUMN duration_hours INT NULL AFTER deadline_mode,
    ADD CONSTRAINT ck_activities_duration_hours
        CHECK (duration_hours IS NULL OR (duration_hours >= 1 AND duration_hours <= 720));
