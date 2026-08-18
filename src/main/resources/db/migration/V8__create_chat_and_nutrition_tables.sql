CREATE TABLE chat_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    response_type VARCHAR(30),
    feedback VARCHAR(20),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_chat_messages_user_created (user_id, created_at),
    CONSTRAINT fk_chat_messages_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE nutrition_contents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_nutrition_contents_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE nutrition_favorites (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    content_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_nutrition_favorites_user_content (user_id, content_id),
    CONSTRAINT fk_nutrition_favorites_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_nutrition_favorites_content FOREIGN KEY (content_id) REFERENCES nutrition_contents (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO nutrition_contents (category, title, summary, body) VALUES
('MEAL_TIMING', '나이트 근무 전 식사, 언제 무엇을 먹을까',
 '나이트 출근 2~3시간 전의 든든한 식사가 근무 중 폭식과 새벽 소화불량을 줄입니다.',
 '나이트 근무일에는 출근 2~3시간 전에 단백질과 복합 탄수화물 중심의 식사를 하세요. 예: 잡곡밥, 닭가슴살이나 생선, 채소 반찬. 근무 중에는 새벽 1~3시 사이에 가벼운 간식(견과류, 요거트, 바나나)으로 허기를 관리하고, 새벽 4시 이후의 무거운 식사는 피하세요. 위장이 소화를 준비하지 못하는 시간대라 역류와 소화불량을 일으키기 쉽습니다. 퇴근 후에는 과식 대신 수프나 죽처럼 부담이 적은 음식을 소량 먹고 수면에 들어가는 것이 낮잠의 질을 지켜줍니다.'),
('MEAL_TIMING', '교대 전환일의 식사 리듬 재설정',
 '식사 시간은 빛만큼 강력한 생체시계 신호입니다. 전환일에는 식사 시간을 새 근무 리듬에 먼저 맞추세요.',
 '근무 유형이 바뀌는 전환일에는 식사 시간을 새 스케줄 기준으로 옮기는 것이 적응을 앞당깁니다. 데이 전환이라면 기상 직후 30분 이내에 아침을 먹어 "하루의 시작" 신호를 주고, 나이트 전환이라면 저녁 식사를 평소보다 2시간 늦춰 밤 시간대 각성을 준비하세요. 전환 당일에는 한 번에 많이 먹기보다 3~4시간 간격의 규칙적인 소식이 위장 부담과 졸림을 줄입니다. 식사 시간이 들쭉날쭉하면 수면 시각을 옮겨도 몸의 시계가 따라오지 못합니다.'),
('CAFFEINE', '카페인 커트라인 계산법',
 '취침 예정 시각에서 자신의 민감도만큼 거슬러 올라간 시각이 마지막 카페인 허용 시각입니다.',
 '카페인의 반감기는 평균 5시간이지만 개인차가 큽니다. 민감도가 낮으면 취침 3시간 전, 보통이면 5시간 전, 높으면 6시간 전을 마지막 카페인 시각으로 잡으세요. 나이트 근무자는 근무 시작 직전~초반의 커피 한 잔은 각성에 도움이 되지만, 퇴근 4~6시간 전부터는 디카페인이나 물로 바꿔야 아침 수면을 지킬 수 있습니다. 에너지음료는 커피보다 카페인 함량 표시가 눈에 잘 띄지 않으니 성분표의 mg 수치를 꼭 확인하세요.'),
('CAFFEINE', '커피 대신 쓸 수 있는 각성 전략',
 '카페인 커트라인 이후 졸림이 온다면 빛·움직임·수분으로 각성을 유지하세요.',
 '커트라인 이후의 졸림에는 카페인 대신 이 세 가지가 효과적입니다. 첫째, 밝은 빛: 휴게 공간의 조명을 최대한 밝게 하거나 5분간 밝은 곳에 다녀오세요. 둘째, 움직임: 계단 오르내리기 2~3분, 어깨·목 스트레칭은 심박수를 올려 즉각적인 각성을 만듭니다. 셋째, 시원한 물 한 잔과 차가운 물로 손 씻기: 가벼운 탈수는 그 자체로 졸림과 두통을 만듭니다. 짧은 낮잠이 허용되는 환경이라면 15~20분 낮잠이 어떤 각성 전략보다 효과적입니다.'),
('HYDRATION', '야간 근무 중 수분 관리',
 '갈증을 느끼기 전에 마시되, 퇴근 2시간 전부터는 줄여서 수면 중 각성을 예방하세요.',
 '야간 근무 중에는 갈증 신호가 둔해져 자신도 모르게 탈수가 진행됩니다. 근무 시작 시 500ml 물병을 채워 2회 이상 비우는 것을 목표로 하고, 1~2시간 간격으로 한 컵씩 나눠 마시세요. 단, 퇴근 2시간 전부터는 섭취량을 줄여야 아침 수면 중 화장실 각성을 줄일 수 있습니다. 국이나 과일도 수분 공급원이지만, 당분이 많은 음료는 혈당 스파이크 후 급격한 졸림을 만들므로 물이나 보리차가 가장 안전합니다.'),
('SUPPLEMENT', '교대근무자와 비타민 D',
 '낮에 자는 생활은 햇빛 노출을 줄입니다. 비타민 D 결핍 여부를 확인하고 보충을 고려하세요.',
 '주로 밤에 일하고 낮에 자는 생활은 햇빛 노출 시간을 크게 줄여 비타민 D 결핍 위험을 높입니다. 비타민 D는 뼈 건강뿐 아니라 면역과 기분 조절에도 관여합니다. 건강검진에서 혈중 25(OH)D 수치를 확인하고, 부족하다면 식품(연어, 달걀노른자, 강화 우유)과 함께 보충제 복용을 고려할 수 있습니다. 다만 복용량과 복용 여부는 반드시 의사·약사와 상의해 결정하세요. 이 콘텐츠는 일반적인 정보이며 개인별 처방을 대신하지 않습니다.'),
('GENERAL', '교대근무자의 장 건강 지키기',
 '불규칙한 식사와 수면은 장 리듬도 흔듭니다. 식이섬유와 규칙적인 식사 간격이 기본입니다.',
 '장도 생체시계를 가지고 있어 교대근무자는 변비·과민성 장 증상을 겪기 쉽습니다. 기본 원칙은 세 가지입니다. 첫째, 근무 형태와 무관하게 "기상 후 첫 식사"를 하루의 기준점으로 삼아 식사 간격을 일정하게 유지하세요. 둘째, 하루 채소·과일·통곡물로 식이섬유 25g 이상을 채우세요. 셋째, 발효식품(요거트, 김치)을 매일 한 가지 이상 포함하세요. 새벽 시간대의 기름진 야식은 장 리듬을 가장 크게 흔드는 요인입니다.'),
('GENERAL', '퇴근 후 수면을 돕는 저녁(아침) 식단',
 '나이트 퇴근 후에는 소화가 쉽고 트립토판이 풍부한 가벼운 식사가 수면을 돕습니다.',
 '나이트 퇴근 후 곧바로 자야 한다면 식사는 "가볍게, 따뜻하게"가 원칙입니다. 트립토판이 풍부한 우유, 바나나, 달걀, 두부는 수면 유도에 도움이 되고, 따뜻한 수프나 죽은 포만감 대비 소화 부담이 적습니다. 반대로 튀김·매운 음식·과음은 수면 중 각성과 역류를 일으킵니다. 술은 잠들기를 도와주는 것처럼 느껴지지만 수면 후반부를 조각내 총 수면의 질을 떨어뜨리므로 퇴근 후 반주는 피하는 것이 좋습니다.');
