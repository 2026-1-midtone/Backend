CREATE TABLE sleep_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    slept_at DATETIME(6) NOT NULL,
    woke_at DATETIME(6) NOT NULL,
    recorded_timezone VARCHAR(64) NOT NULL,
    source VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_sleep_logs_user_woke_at (user_id, woke_at),
    CONSTRAINT fk_sleep_logs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_sleep_logs_interval CHECK (woke_at > slept_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE caffeine_intakes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    consumed_at DATETIME(6) NOT NULL,
    recorded_timezone VARCHAR(64) NOT NULL,
    amount_mg INT NOT NULL,
    servings DECIMAL(4,2) NOT NULL DEFAULT 1.00,
    beverage_type VARCHAR(50),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_caffeine_intakes_user_consumed_at (user_id, consumed_at),
    CONSTRAINT fk_caffeine_intakes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_caffeine_intakes_amount CHECK (amount_mg > 0),
    CONSTRAINT chk_caffeine_intakes_servings CHECK (servings > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
