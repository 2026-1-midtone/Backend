UPDATE nutrition_contents SET timing_tag = 'BEFORE_NIGHT'
WHERE title = '나이트 근무 전 식사, 언제 무엇을 먹을까';

UPDATE nutrition_contents SET timing_tag = 'DURING_NIGHT'
WHERE title IN ('카페인 커트라인 계산법', '커피 대신 쓸 수 있는 각성 전략', '야간 근무 중 수분 관리');

UPDATE nutrition_contents SET timing_tag = 'AFTER_NIGHT'
WHERE title = '퇴근 후 수면을 돕는 저녁(아침) 식단';
