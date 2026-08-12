CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    google_subject VARCHAR(255) NOT NULL,
    email VARCHAR(320) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    profile_image_url VARCHAR(2048),
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Seoul',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_google_subject (google_subject),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
