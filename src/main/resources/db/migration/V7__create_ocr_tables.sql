CREATE TABLE ocr_jobs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    image MEDIUMBLOB,
    image_mime VARCHAR(30),
    target_month VARCHAR(7) NOT NULL,
    error_message VARCHAR(255),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_ocr_jobs_user (user_id),
    CONSTRAINT fk_ocr_jobs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ocr_draft_shifts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    shift_type VARCHAR(20) NOT NULL,
    start_time TIME,
    end_time TIME,
    confidence DECIMAL(4,3),
    excluded BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ocr_draft_shifts_job_date (job_id, work_date),
    CONSTRAINT fk_ocr_draft_shifts_job FOREIGN KEY (job_id) REFERENCES ocr_jobs (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
