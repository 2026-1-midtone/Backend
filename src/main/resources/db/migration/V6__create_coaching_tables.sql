CREATE TABLE daily_coachings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    coaching_date DATE NOT NULL,
    today_shift_type VARCHAR(20) NOT NULL,
    next_shift_start_at DATETIME(6),
    caffeine_sensitivity_used VARCHAR(20) NOT NULL,
    is_transition_day BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_daily_coachings_user_date (user_id, coaching_date),
    CONSTRAINT fk_daily_coachings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE coaching_cards (
    id BIGINT NOT NULL AUTO_INCREMENT,
    daily_coaching_id BIGINT NOT NULL,
    card_type VARCHAR(30) NOT NULL,
    title VARCHAR(50) NOT NULL,
    window_start DATETIME(6) NOT NULL,
    window_end DATETIME(6) NOT NULL,
    description VARCHAR(255) NOT NULL,
    rationale VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_coaching_cards_daily_coaching (daily_coaching_id),
    CONSTRAINT fk_coaching_cards_daily_coaching FOREIGN KEY (daily_coaching_id) REFERENCES daily_coachings (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
