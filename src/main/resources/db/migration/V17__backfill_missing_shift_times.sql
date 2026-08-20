-- 근무 유형별 기본 시각으로 빈 시각을 채우는 처리(V14)가 들어오기 전에 저장된 근무표는
-- start_time/end_time 이 비어 있다. 시각이 없으면 코칭 카드·다음 근무 카운트다운·
-- 야간 영양 타이밍이 전부 계산되지 않아, 근무표가 등록돼 있어도 코칭 화면이
-- "일정 미정 / 보여드릴 코칭이 아직 없어요"로만 보인다.
-- 사용자가 정해 둔 유형별 기본값이 있으면 그것을, 없으면 ShiftTimeDefaults 의 대표값을 쓴다.
-- OFF 는 근무가 아니라 시각이 없는 것이 맞으므로 그대로 둔다.
UPDATE shift_schedules s
LEFT JOIN user_shift_time_defaults d
    ON d.user_id = s.user_id AND d.shift_type = s.shift_type
SET s.start_time = COALESCE(s.start_time, d.start_time, CASE s.shift_type
        WHEN 'DAY' THEN '09:00:00'
        WHEN 'EVENING' THEN '12:00:00'
        WHEN 'NIGHT' THEN '22:00:00'
    END),
    s.end_time = COALESCE(s.end_time, d.end_time, CASE s.shift_type
        WHEN 'DAY' THEN '18:00:00'
        WHEN 'EVENING' THEN '21:00:00'
        WHEN 'NIGHT' THEN '07:00:00'
    END)
WHERE s.shift_type <> 'OFF'
  AND (s.start_time IS NULL OR s.end_time IS NULL);

-- 코칭은 날짜별로 한 번만 만들어 두고 다시 읽어 쓰기 때문에, 시각을 채워도 그동안 카드 없이
-- 저장된 코칭이 그대로 남아 화면은 계속 비어 보인다. 근무 유형이 OFF 가 아닌 날은 카드가
-- 반드시 세 장 나오므로, 카드가 하나도 없는 코칭은 시각이 비었던 시절의 잔재로 보고 지운다.
-- 다음 조회 때 다시 만들어진다. 지난 날짜는 루틴 실행 기록이 함께 지워지므로 건드리지 않는다.
DELETE dc FROM daily_coachings dc
JOIN shift_schedules s
    ON s.user_id = dc.user_id AND s.work_date = dc.coaching_date
LEFT JOIN coaching_cards cc
    ON cc.daily_coaching_id = dc.id
WHERE dc.coaching_date >= CURRENT_DATE
  AND s.shift_type <> 'OFF'
  AND cc.id IS NULL;
