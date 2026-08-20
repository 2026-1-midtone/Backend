-- =============================================================================
-- 시연용 계정 데이터 시드 스크립트
-- =============================================================================
-- 목적 : 시연 계정에 4주(28일) 이상의 근무표와 수면·카페인·낮잠 기록을 넣어
--        코칭 카드 / 루틴 항목 / 전환일 / 낮잠 타이머 화면이 비어 보이지 않게 한다.
--
-- 실행 전 반드시 확인할 것
--   1. 시연 계정으로 앱에서 구글 로그인을 **한 번 먼저** 해야 한다.
--      로그인 시점에 users 행이 생성되며, 이 스크립트는 그 행을 찾아서 데이터를 붙인다.
--   2. 아래 @demo_email 을 시연 계정 이메일로 바꾼다.
--   3. 이 스크립트는 @demo_email 계정의 기존 근무표·코칭·루틴·수면·카페인·낮잠·영양소 목표 데이터를
--      **삭제하고 다시 넣는다**. 실제 사용자 계정에는 절대 실행하지 말 것.
--   4. 날짜는 실행 시점의 CURDATE() 기준으로 계산된다. 시연 당일에 실행하면 가장 정확하다.
--
-- 실행 후 할 일은 docs/demo-setup.md 참고 (코칭·루틴 생성 API 호출이 남아 있다).
-- =============================================================================

-- ── 0. 대상 계정 지정 ────────────────────────────────────────────────────────
SET @demo_email = 'CHANGE_ME@example.com';

-- CONVERT + COLLATE 를 쓰는 이유:
--   users.email 은 utf8mb4_unicode_ci 인데 사용자 변수는 접속 기본 collation 을 따라간다.
--   - 접속이 utf8mb4 면 그냥 비교 시 "Illegal mix of collations" (ERROR 1267)
--   - 접속이 latin1 이면 COLLATE 만 붙였을 때 ERROR 1253
--   CONVERT(... USING utf8mb4) 로 문자셋을 먼저 맞추면 두 경우 다 안전하다.
SET @demo_user_id = (SELECT id FROM users
                     WHERE email = CONVERT(@demo_email USING utf8mb4) COLLATE utf8mb4_unicode_ci);

-- 아래 SELECT 결과가 NULL 이면 여기서 멈추고 구글 로그인부터 다시 할 것.
-- (NULL 인 채로 진행하면 user_id NOT NULL 제약에 걸려 실패한다.)
SELECT @demo_user_id AS demo_user_id, @demo_email AS demo_email;

-- ── 1. 사용자 설정 ───────────────────────────────────────────────────────────
INSERT INTO user_settings (user_id, caffeine_daily_mg, caffeine_sensitivity, preferred_nap_minutes, max_naps_per_day)
VALUES (@demo_user_id, 400, 'MEDIUM', 20, 2)
ON DUPLICATE KEY UPDATE
    caffeine_daily_mg = VALUES(caffeine_daily_mg),
    caffeine_sensitivity = VALUES(caffeine_sensitivity),
    preferred_nap_minutes = VALUES(preferred_nap_minutes),
    max_naps_per_day = VALUES(max_naps_per_day);

INSERT INTO notification_settings (user_id, type, enabled, custom_time)
VALUES
    (@demo_user_id, 'NAP', TRUE, '14:30:00'),
    (@demo_user_id, 'CAFFEINE_CUTOFF', TRUE, NULL),
    (@demo_user_id, 'LIGHT_EXPOSURE', TRUE, NULL)
ON DUPLICATE KEY UPDATE enabled = VALUES(enabled), custom_time = VALUES(custom_time);

-- ── 2. 기존 시연 데이터 정리 (시연 계정 한정) ────────────────────────────────
DELETE FROM coaching_cards
WHERE daily_coaching_id IN (SELECT id FROM daily_coachings WHERE user_id = @demo_user_id);
DELETE FROM daily_coachings WHERE user_id = @demo_user_id;
DELETE FROM routine_tasks WHERE user_id = @demo_user_id;
DELETE FROM nap_sessions WHERE user_id = @demo_user_id;
DELETE FROM sleep_logs WHERE user_id = @demo_user_id;
DELETE FROM caffeine_intakes WHERE user_id = @demo_user_id;
DELETE FROM shift_schedules WHERE user_id = @demo_user_id;
DELETE FROM user_nutrient_needs WHERE user_id = @demo_user_id;

-- ── 3. 근무표 42일 (오늘 기준 -14일 ~ +27일) ─────────────────────────────────
-- 앞으로 28일(오늘 ~ +27일)이 모두 차 있으므로 /api/v1/shifts/completeness?weeks=4 가 100% 가 된다.
-- 7일 주기 D-D-E-E-N-N-OFF 로 돌기 때문에 D→E, E→N, N→D 지점마다 전환일이 잡힌다.
-- @cycle_offset 값과 무관하게 **오늘은 항상 전환일**이 되도록 주기를 맞춰 두었다.
--
-- @cycle_offset 으로 "오늘 근무"를 고른다. 홈 대시보드의 topCoachingCards 는
-- **아직 끝나지 않은 카드 창(window)만** 내려주기 때문에, 시연 시각에 맞춰 골라야 카드가 보인다.
--   0 → 오늘 DAY     : 카드 창 03:00~11:00  (오전 시연용)
--   2 → 오늘 EVENING : 카드 창 11:00~19:00  (오후 시연용)
--   4 → 오늘 NIGHT   : 카드 창 19:00~24:00  (저녁·야간 시연용)
-- 창이 다 지난 뒤라도 GET /api/v1/coachings 에는 카드가 그대로 나온다. 비는 건 홈 상단 요약뿐이다.
SET @cycle_offset = 0;

INSERT INTO shift_schedules (user_id, work_date, shift_type, start_time, end_time, source, confidence, confirmed)
SELECT
    @demo_user_id,
    d.work_date,
    d.shift_type,
    CASE d.shift_type WHEN 'DAY' THEN '07:00:00' WHEN 'EVENING' THEN '15:00:00' WHEN 'NIGHT' THEN '23:00:00' END,
    CASE d.shift_type WHEN 'DAY' THEN '15:00:00' WHEN 'EVENING' THEN '23:00:00' WHEN 'NIGHT' THEN '07:00:00' END,
    'MANUAL',
    NULL,
    TRUE
FROM (
    SELECT
        DATE_ADD(CURDATE(), INTERVAL seq.n - 14 DAY) AS work_date,
        ELT((seq.n + @cycle_offset) MOD 7 + 1, 'DAY', 'DAY', 'EVENING', 'EVENING', 'NIGHT', 'NIGHT', 'OFF') AS shift_type
    FROM (
        SELECT ones.n + tens.n * 10 AS n
        FROM (
            SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
            UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
        ) ones
        CROSS JOIN (
            SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        ) tens
    ) seq
    WHERE seq.n < 42
) d;

-- ── 4. 수면 기록 (최근 14일) ─────────────────────────────────────────────────
-- 근무 유형에 따라 취침 시각을 다르게 넣어 수면 패턴 그래프가 의미 있게 보이도록 한다.
INSERT INTO sleep_logs (user_id, slept_at, woke_at, recorded_timezone, source)
SELECT
    @demo_user_id,
    CASE s.shift_type
        WHEN 'NIGHT' THEN TIMESTAMP(s.work_date, '08:30:00')
        WHEN 'EVENING' THEN TIMESTAMP(DATE_SUB(s.work_date, INTERVAL 1 DAY), '01:00:00')
        ELSE TIMESTAMP(DATE_SUB(s.work_date, INTERVAL 1 DAY), '23:00:00')
    END,
    CASE s.shift_type
        WHEN 'NIGHT' THEN TIMESTAMP(s.work_date, '14:00:00')
        WHEN 'EVENING' THEN TIMESTAMP(s.work_date, '09:00:00')
        ELSE TIMESTAMP(s.work_date, '06:00:00')
    END,
    'Asia/Seoul',
    'MANUAL'
FROM shift_schedules s
WHERE s.user_id = @demo_user_id
  AND s.work_date BETWEEN DATE_SUB(CURDATE(), INTERVAL 14 DAY) AND DATE_SUB(CURDATE(), INTERVAL 1 DAY);

-- ── 5. 카페인 섭취 기록 (최근 7일, 하루 2잔) ─────────────────────────────────
INSERT INTO caffeine_intakes (user_id, consumed_at, recorded_timezone, amount_mg, servings, beverage_type)
SELECT
    @demo_user_id,
    TIMESTAMP(s.work_date, t.consumed_time),
    'Asia/Seoul',
    t.amount_mg,
    1.00,
    t.beverage_type
FROM shift_schedules s
CROSS JOIN (
    SELECT '08:30:00' AS consumed_time, 150 AS amount_mg, '아메리카노' AS beverage_type
    UNION ALL
    SELECT '13:30:00', 75, '아이스티'
) t
WHERE s.user_id = @demo_user_id
  AND s.shift_type <> 'OFF'
  AND s.work_date BETWEEN DATE_SUB(CURDATE(), INTERVAL 7 DAY) AND DATE_SUB(CURDATE(), INTERVAL 1 DAY);

-- ── 6. 낮잠 기록 (최근 3일, 완료된 세션) ─────────────────────────────────────
-- 진행 중(RUNNING) 세션은 넣지 않는다. 시연 때 POST /api/v1/naps 로 직접 타이머를 시작해야
-- 낮잠 타이머 화면을 실제 동작으로 보여줄 수 있고, RUNNING 이 미리 있으면 시작이 막힌다.
INSERT INTO nap_sessions (user_id, planned_minutes, started_at, expected_end_at, ended_at, status)
VALUES
    (@demo_user_id, 20, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '14:10:00'),
        TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '14:30:00'),
        TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '14:32:00'), 'COMPLETED'),
    (@demo_user_id, 20, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '13:40:00'),
        TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '14:00:00'),
        TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '14:01:00'), 'COMPLETED'),
    (@demo_user_id, 30, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '15:00:00'),
        TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '15:30:00'),
        TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '15:28:00'), 'COMPLETED');

-- ── 7. 영양소 목표 ──────────────────────────────────────────────
-- 영양소 목표가 한 건도 없으면 POST /api/v1/chat/messages:recommend-products 가 LLM 까지 가지 않고
-- "아직 등록된 영양소 목표가 없어 추천할 제품을 찾지 못했어요" 를 고정 반환한다.
-- 아래 3개는 V10 이 심는 제품 3종을 모두 걸리게 하려고 골랐다.
--   MAGNESIUM  -> 바이브젠 딥 슬립 앤 비전
--   VITAMIN_C  -> 바이브젠 바이탈 스킨 샷 (리바이브 에너지 샷에도 있음)
--   VITAMIN_D  -> 바이브젠 리바이브 에너지 샷
INSERT INTO user_nutrient_needs (user_id, nutrient_code, source, recorded_on)
VALUES
    (@demo_user_id, 'MAGNESIUM', 'USER_REPORTED', CURDATE()),
    (@demo_user_id, 'VITAMIN_C', 'USER_REPORTED', CURDATE()),
    (@demo_user_id, 'VITAMIN_D', 'USER_REPORTED', CURDATE())
ON DUPLICATE KEY UPDATE
    source = VALUES(source),
    recorded_on = VALUES(recorded_on);

-- ── 8. 결과 확인 ─────────────────────────────────────────────────────────────
SELECT '근무표 총 일수' AS item, COUNT(*) AS value
FROM shift_schedules WHERE user_id = @demo_user_id
UNION ALL
SELECT '앞으로 28일 확정 일수 (28 이어야 함)', COUNT(*)
FROM shift_schedules
WHERE user_id = @demo_user_id
  AND confirmed = TRUE
  AND work_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 27 DAY)
UNION ALL
SELECT CONCAT('오늘 근무 (', IFNULL((SELECT shift_type FROM shift_schedules
    WHERE user_id = @demo_user_id AND work_date = CURDATE()), '없음'), ') - OFF 아니어야 코칭 200'), COUNT(*)
FROM shift_schedules
WHERE user_id = @demo_user_id AND work_date = CURDATE() AND shift_type <> 'OFF'
UNION ALL
SELECT '수면 기록', COUNT(*) FROM sleep_logs WHERE user_id = @demo_user_id
UNION ALL
SELECT '카페인 기록', COUNT(*) FROM caffeine_intakes WHERE user_id = @demo_user_id
UNION ALL
SELECT '낮잠 기록', COUNT(*) FROM nap_sessions WHERE user_id = @demo_user_id
UNION ALL
SELECT '영양소 목표 (0 이면 제품 추천이 동작하지 않음)', COUNT(*)
FROM user_nutrient_needs WHERE user_id = @demo_user_id;

SELECT work_date, shift_type, start_time, end_time
FROM shift_schedules
WHERE user_id = @demo_user_id
  AND work_date BETWEEN DATE_SUB(CURDATE(), INTERVAL 3 DAY) AND DATE_ADD(CURDATE(), INTERVAL 6 DAY)
ORDER BY work_date;
