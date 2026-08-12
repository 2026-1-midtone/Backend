CREATE TABLE user_settings (
    user_id BIGINT NOT NULL,
    caffeine_daily_mg INT,
    caffeine_sensitivity VARCHAR(20),
    preferred_nap_minutes INT NOT NULL DEFAULT 20,
    max_naps_per_day INT NOT NULL DEFAULT 2,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_settings_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE routine_tasks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    task_date DATE NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    source_id BIGINT,
    category VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    tip VARCHAR(1000),
    window_start DATETIME(6) NOT NULL,
    window_end DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    completed_at DATETIME(6),
    PRIMARY KEY (id),
    KEY idx_routine_tasks_user_date (user_id, task_date),
    CONSTRAINT fk_routine_tasks_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE nap_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    planned_minutes INT NOT NULL,
    started_at DATETIME(6) NOT NULL,
    expected_end_at DATETIME(6) NOT NULL,
    ended_at DATETIME(6),
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_nap_sessions_user_status (user_id, status),
    CONSTRAINT fk_nap_sessions_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
