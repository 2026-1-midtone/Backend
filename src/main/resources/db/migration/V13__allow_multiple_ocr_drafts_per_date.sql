-- 한 달력 셀에 배지가 두 개 쌓인 근무표(예: 오프+나이트)를 지원하기 위해
-- OCR 초안의 유니크 키를 (job_id, work_date)에서 (job_id, work_date, shift_type)으로 완화한다.
ALTER TABLE ocr_draft_shifts
    DROP KEY uk_ocr_draft_shifts_job_date,
    ADD UNIQUE KEY uk_ocr_draft_shifts_job_date_type (job_id, work_date, shift_type);
