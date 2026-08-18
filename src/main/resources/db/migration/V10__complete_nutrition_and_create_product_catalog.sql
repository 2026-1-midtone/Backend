ALTER TABLE nutrition_contents
    ADD COLUMN content_type VARCHAR(30) NOT NULL DEFAULT 'NUTRITION_INFO' AFTER category,
    ADD COLUMN timing_tag VARCHAR(30) NOT NULL DEFAULT 'GENERAL' AFTER content_type,
    ADD COLUMN thumbnail_url VARCHAR(2048) NULL AFTER body,
    ADD COLUMN source_url VARCHAR(2048) NULL AFTER thumbnail_url,
    ADD COLUMN disclaimer VARCHAR(500) NOT NULL DEFAULT '건강기능식품은 질병의 예방 및 치료를 위한 의약품이 아니며, 일반적인 참고 정보입니다.' AFTER source_url,
    ADD KEY idx_nutrition_contents_filters (timing_tag, content_type);

CREATE TABLE nutrition_products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    english_name VARCHAR(200) NOT NULL,
    disclaimer VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_nutrition_products_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE nutrition_product_functions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    ingredient VARCHAR(100) NOT NULL,
    nutrient_code VARCHAR(50) NULL,
    function_info VARCHAR(500) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_function_order (product_id, sort_order),
    KEY idx_product_functions_nutrient (nutrient_code, product_id),
    CONSTRAINT fk_product_functions_product FOREIGN KEY (product_id) REFERENCES nutrition_products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_nutrient_needs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    nutrient_code VARCHAR(50) NOT NULL,
    source VARCHAR(30) NOT NULL,
    recorded_on DATE NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_nutrient_needs_user_code (user_id, nutrient_code),
    KEY idx_user_nutrient_needs_user_recorded (user_id, recorded_on),
    CONSTRAINT fk_user_nutrient_needs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO nutrition_products (code, name, english_name, disclaimer) VALUES
('DEEP_SLEEP_VISION', '바이브젠 딥 슬립 앤 비전', 'VIVEGEN DEEP SLEEP & VISION',
 '건강기능식품은 질병의 예방 및 치료를 위한 의약품이 아닙니다. 제품 표시사항과 섭취 시 주의사항을 확인하고, 의약품 복용·질환·임신·수유 중에는 전문가와 상담하세요.'),
('VITAL_SKIN_SHOT', '바이브젠 바이탈 스킨 샷', 'VIVEGEN VITAL SKIN SHOT',
 '건강기능식품은 질병의 예방 및 치료를 위한 의약품이 아닙니다. 제품 표시사항과 섭취 시 주의사항을 확인하고, 의약품 복용·질환·임신·수유 중에는 전문가와 상담하세요.'),
('REVIVE_ENERGY_SHOT', '바이브젠 리바이브 에너지 샷', 'VIVEGEN REVIVE ENERGY SHOT',
 '건강기능식품은 질병의 예방 및 치료를 위한 의약품이 아닙니다. 제품 표시사항과 섭취 시 주의사항을 확인하고, 의약품 복용·질환·임신·수유 중에는 전문가와 상담하세요.');

INSERT INTO nutrition_product_functions (product_id, ingredient, nutrient_code, function_info, sort_order)
SELECT id, '유단백가수분해물(락티움)', NULL, '수면의 질 개선', 1 FROM nutrition_products WHERE code = 'DEEP_SLEEP_VISION'
UNION ALL SELECT id, '테아닌', NULL, '스트레스로 인한 긴장완화', 2 FROM nutrition_products WHERE code = 'DEEP_SLEEP_VISION'
UNION ALL SELECT id, '마리골드꽃추출물(지아잔틴함유)', NULL, '노화로 인해 감소될 수 있는 황반색소밀도 유지, 눈 건강', 3 FROM nutrition_products WHERE code = 'DEEP_SLEEP_VISION'
UNION ALL SELECT id, '헤마토코쿠스 추출물', NULL, '눈의 피로도 개선', 4 FROM nutrition_products WHERE code = 'DEEP_SLEEP_VISION'
UNION ALL SELECT id, '마그네슘', 'MAGNESIUM', '에너지 이용, 신경과 근육 기능 유지', 5 FROM nutrition_products WHERE code = 'DEEP_SLEEP_VISION'
UNION ALL SELECT id, '비타민B6', 'VITAMIN_B6', '단백질 및 아미노산 이용, 혈액 호모시스테인 수준 정상 유지', 6 FROM nutrition_products WHERE code = 'DEEP_SLEEP_VISION'
UNION ALL SELECT id, '아연', 'ZINC', '정상적인 면역기능, 정상적인 세포분열', 7 FROM nutrition_products WHERE code = 'DEEP_SLEEP_VISION';

INSERT INTO nutrition_product_functions (product_id, ingredient, nutrient_code, function_info, sort_order)
SELECT id, 'Collactive 콜라겐펩타이드', NULL, '피부 보습', 1 FROM nutrition_products WHERE code = 'VITAL_SKIN_SHOT'
UNION ALL SELECT id, '히알루론산', NULL, '피부 보습', 2 FROM nutrition_products WHERE code = 'VITAL_SKIN_SHOT'
UNION ALL SELECT id, '비타민A', 'VITAMIN_A', '어두운 곳에서 시각 적응, 피부와 점막 형성·기능 유지, 상피세포 성장·발달', 3 FROM nutrition_products WHERE code = 'VITAL_SKIN_SHOT'
UNION ALL SELECT id, '비타민C', 'VITAMIN_C', '결합조직 형성·기능유지, 철의 흡수, 항산화 작용', 4 FROM nutrition_products WHERE code = 'VITAL_SKIN_SHOT'
UNION ALL SELECT id, '셀렌', 'SELENIUM', '유해산소로부터 세포 보호', 5 FROM nutrition_products WHERE code = 'VITAL_SKIN_SHOT'
UNION ALL SELECT id, '비오틴', 'BIOTIN', '지방·탄수화물·단백질 대사와 에너지 생성', 6 FROM nutrition_products WHERE code = 'VITAL_SKIN_SHOT';

INSERT INTO nutrition_product_functions (product_id, ingredient, nutrient_code, function_info, sort_order)
SELECT id, '밀크씨슬 추출물', NULL, '간 건강', 1 FROM nutrition_products WHERE code = 'REVIVE_ENERGY_SHOT'
UNION ALL SELECT id, '코엔자임Q10', NULL, '항산화·높은 혈압 감소', 2 FROM nutrition_products WHERE code = 'REVIVE_ENERGY_SHOT'
UNION ALL SELECT id, '마그네슘', 'MAGNESIUM', '에너지 이용, 신경과 근육 기능 유지', 3 FROM nutrition_products WHERE code = 'REVIVE_ENERGY_SHOT'
UNION ALL SELECT id, '비타민E', 'VITAMIN_E', '항산화 작용', 4 FROM nutrition_products WHERE code = 'REVIVE_ENERGY_SHOT'
UNION ALL SELECT id, '비타민C', 'VITAMIN_C', '결합조직 형성·기능유지, 철의 흡수, 항산화 작용', 5 FROM nutrition_products WHERE code = 'REVIVE_ENERGY_SHOT'
UNION ALL SELECT id, '비타민B1', 'VITAMIN_B1', '탄수화물과 에너지 대사', 6 FROM nutrition_products WHERE code = 'REVIVE_ENERGY_SHOT'
UNION ALL SELECT id, '비타민B6', 'VITAMIN_B6', '단백질 및 아미노산 이용, 혈액 호모시스테인 수준 정상 유지', 7 FROM nutrition_products WHERE code = 'REVIVE_ENERGY_SHOT'
UNION ALL SELECT id, '판토텐산', 'PANTOTHENIC_ACID', '지방·탄수화물·단백질 대사와 에너지 생성', 8 FROM nutrition_products WHERE code = 'REVIVE_ENERGY_SHOT'
UNION ALL SELECT id, '비타민B2', 'VITAMIN_B2', '체내 에너지 생성', 9 FROM nutrition_products WHERE code = 'REVIVE_ENERGY_SHOT'
UNION ALL SELECT id, '나이아신', 'NIACIN', '체내 에너지 생성', 10 FROM nutrition_products WHERE code = 'REVIVE_ENERGY_SHOT'
UNION ALL SELECT id, '비타민B12', 'VITAMIN_B12', '정상적인 엽산 대사', 11 FROM nutrition_products WHERE code = 'REVIVE_ENERGY_SHOT'
UNION ALL SELECT id, '비타민D', 'VITAMIN_D', '칼슘과 인이 흡수·이용, 뼈의 형성과 유지, 골다공증발생 위험 감소', 12 FROM nutrition_products WHERE code = 'REVIVE_ENERGY_SHOT'
UNION ALL SELECT id, '엽산', 'FOLATE', '세포와 혈액생성, 태아의 신경관 정상 발달, 혈액 호모시스테인 수준 정상 유지', 13 FROM nutrition_products WHERE code = 'REVIVE_ENERGY_SHOT';
