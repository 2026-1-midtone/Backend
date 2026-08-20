-- 근무 유형별 기본 시작·종료 시각.
-- 시각이 비어 있는 근무는 코칭 카드·다음 근무·야간 영양 타이밍이 모두 계산되지 않는데,
-- 지금까지는 사용자가 유형별 시각을 정할 방법이 없어 매번 직접 입력해야 했다.
CREATE TABLE user_shift_time_defaults (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    shift_type VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_shift_time_defaults_user_type (user_id, shift_type),
    CONSTRAINT fk_user_shift_time_defaults_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
