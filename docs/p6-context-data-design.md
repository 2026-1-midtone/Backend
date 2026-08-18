# P6 컨텍스트 데이터·채팅·영양 API 설계

## 목표와 원칙

수면 및 카페인 실측 기록을 저장하고, 백엔드가 계산한 값만 AI 챗봇의 `context_snapshot`에 주입한다. LLM은 시각·용량·권장 창을 계산하거나 추정하지 않으며, 근거 발췌와 계산 결과를 자연어로 설명한다.

이번 범위는 다음 네 부분이다.

1. 수면 기록 저장 및 조회·수정·삭제
2. 카페인 섭취 기록 저장 및 조회·수정·삭제
3. 기록을 반영한 계산 스냅샷과 채팅 API
4. 기존 영양 도메인을 API 명세에 맞게 완성

OCR은 현재 구현을 유지한다. 업로드, 비동기 분석, 상태 조회, 초안 보정, 확정, 재시도 기능은 동작한다. 첨부 API 명세와 URL·응답 형식은 다르지만 P5 기능 결함으로 보지 않으며 이번 범위에서 변경하지 않는다.

## 데이터 모델

기존 마이그레이션을 수정하지 않고 신규 Flyway 마이그레이션을 추가한다.

### `sleep_logs`

| 열 | 형식 | 설명 |
| --- | --- | --- |
| `id` | BIGINT PK | 기록 ID |
| `user_id` | BIGINT FK | 사용자 |
| `slept_at` | DATETIME(6) | 사용자의 현지 취침 시각 |
| `woke_at` | DATETIME(6) | 사용자의 현지 기상 시각 |
| `recorded_timezone` | VARCHAR(64) | 기록 당시 IANA 타임존 |
| `source` | VARCHAR(20) | `MANUAL` 또는 향후 `DEVICE` |
| `created_at`, `updated_at` | DATETIME(6) | 생성·수정 시각 |

한 사용자는 하루에 여러 수면 구간을 기록할 수 있다. 교대근무자의 분할 수면을 보존하기 위해 날짜당 한 건 제한을 두지 않는다. 동일 사용자의 서로 겹치는 수면 구간은 409로 거절한다.

### `caffeine_intakes`

| 열 | 형식 | 설명 |
| --- | --- | --- |
| `id` | BIGINT PK | 기록 ID |
| `user_id` | BIGINT FK | 사용자 |
| `consumed_at` | DATETIME(6) | 사용자의 현지 섭취 시각 |
| `recorded_timezone` | VARCHAR(64) | 기록 당시 IANA 타임존 |
| `amount_mg` | INT | 사용자가 입력한 카페인 용량 |
| `servings` | DECIMAL(4,2) | 잔 수 상한선 계산용 섭취 잔 수 |
| `beverage_type` | VARCHAR(50) NULL | 선택 입력 음료 유형 |
| `created_at`, `updated_at` | DATETIME(6) | 생성·수정 시각 |

`amount_mg`와 `servings`는 모두 양수여야 한다. mg에서 잔 수를 임의 환산하지 않고 사용자가 입력한 `servings`를 그대로 합산한다.

## 기록 API

Base URL은 `/api/v1`이며 기존 JWT 사용자 식별과 공통 오류 형식을 사용한다.

### 수면 기록

- `POST /sleep-logs`: `{ sleptAt, wokeAt, source? }` 등록, 201
- `GET /sleep-logs?from=&to=`: 기상일 기준 기간 조회, 오래된 순
- `PATCH /sleep-logs/{sleepLogId}`: 소유 기록 보정
- `DELETE /sleep-logs/{sleepLogId}`: 소유 기록 삭제, 204

검증은 `wokeAt > sleptAt`, 미래 기록 금지, 타인 소유 403, 대상 없음 404, 겹침 409를 적용한다.

### 카페인 기록

- `POST /caffeine-intakes`: `{ consumedAt, amountMg, servings?, beverageType? }` 등록, 201
- `GET /caffeine-intakes?from=&to=`: 기간 조회와 `totalAmountMg`, `totalServings` 반환
- `PATCH /caffeine-intakes/{intakeId}`: 소유 기록 보정
- `DELETE /caffeine-intakes/{intakeId}`: 소유 기록 삭제, 204

`servings` 기본값은 1이며, 미래 섭취 기록은 거절한다. 금지·권장 용량을 API 검증 규칙으로 만들지 않는다.

## 백엔드 계산 계층

### 수면 패턴

`SleepPatternCalculator`가 현재 날짜 이전 최근 14일의 모든 기록을 사용한다. 최소 로그 개수 제한은 두지 않는다.

- 습관적 취침시각: 취침 시각의 이동평균
- 습관적 기상시각: 기상 시각의 이동평균
- 습관적 중간수면: 각 기록의 `취침 + 수면시간 / 2`를 구한 뒤 이동평균
- CBTmin 근사치: 습관적 기상시각에서 문서에 명시된 약 2시간을 뺀 값

자정을 넘는 시각의 단순 산술평균 오류를 피하도록 시각 평균은 원형 평균으로 계산한다. 결과에는 사용한 로그 수와 기간을 함께 넣어 데이터가 적을 때도 값의 근거를 드러낸다.

### 카페인 상태

`CaffeineStatusCalculator`가 사용자의 현지 날짜별 `amount_mg`와 `servings`를 합산한다.

- 컷오프 시각은 백엔드가 습관적 취침시각 또는 기존 근무표 앵커로 계산한다.
- 습관적 취침시각이 있으면 이를 우선 사용하고, 없으면 기존 근무표 계산으로 폴백한다.
- 하루 잔 수가 문서의 상한선을 초과했는지는 `servings` 합계로 판정한다.
- 개별 섭취량의 체내 잔존량 계산은 이번 범위에서 제외한다.

### 전환일

현재 고정 시각 프로토콜을 다음 근무 시작 기준 상대 오프셋으로 표현할 수 있는 내부 모델을 추가한다. 기존 응답 계약은 유지하고, 습관적 중간수면이 있을 때만 앵커 보정을 적용한다. 근거가 없는 낮잠 수면압력 임계치는 추가하지 않는다.

## `context_snapshot`

`ChatContextSnapshotBuilder`는 문자열을 직접 조립하지 않고 구조화 객체를 만든다.

- 오늘 및 최근 근무 일정
- 오늘 코칭 카드의 카페인 컷오프, 빛 노출 권장·지양 창, 낮잠 창
- 오늘 카페인 총 mg·잔 수와 상한선 경고 상태
- 최근 수면 기록, 습관적 취침·기상·중간수면, 계산에 사용한 로그 수
- 카페인 민감도와 낮잠 설정
- 오늘 루틴 완료 상태
- 전환일 여부와 단계

값이 없으면 `null` 또는 빈 목록으로 전달하고, 추정 기본값을 사실처럼 채우지 않는다.

## 채팅 파이프라인

1. 사용자 메시지를 저장한다.
2. 결정적 안전 규칙으로 명백한 응급·의료 신호를 먼저 차단한다.
3. 질문을 `CAFFEINE`, `LIGHT`, `NAP`, `TRANSITION`, `NUTRITION` 중 하나 또는 미분류로 매칭한다.
4. 해당 도메인의 두 기준 문서 근거 발췌만 선택한다.
5. `context_snapshot`, 근거 발췌, 최근 대화를 OpenAI Responses API에 전달한다.
6. strict Structured Outputs로 `answer_text`, `safety_flag`, `cited_domain`을 받는다.
7. 안전 분류와 스키마를 재검증하고 어시스턴트 메시지를 저장한다.
8. 외부 API 응답은 기존 명세의 `answer`, `verdict`, `reasons`, `alternatives`, `context`, `safetyFlag` 형태로 조합한다. `verdict`, 이유, 대안, 숫자 컨텍스트는 백엔드 계산 결과에서만 만든다.

시스템 프롬프트는 숫자 생성 금지, 근거 범위 제한, 안전 분류, 진단·처방 금지만 포함한다. 답변 톤이나 문장 템플릿은 강제하지 않는다.

OpenAI 모델은 환경변수로 교체 가능하게 하고 기본값을 mini급 모델로 둔다. API 키가 없으면 애플리케이션은 기동하되 채팅 요청만 502를 반환한다.

채팅 모델 공급자는 `ChatAnswerGenerator` 포트 뒤에서 교체한다. `GEMINI_API_KEY`가 설정되면 Gemini(`generateContent` + responseSchema 구조화 출력, 기본 `gemini-2.5-flash`)를, 없으면 OpenAI를 사용한다. 두 공급자는 동일한 시스템 프롬프트와 입력 조립(`ChatPromptTexts`)을 공유한다. OCR 폴백은 공급자 선택과 무관하게 `OPENAI_API_KEY`를 따로 사용한다.

### 제품 추천 전용 엔드포인트

`POST /api/v1/chat/messages:recommend-products` (본문 없음). 프론트의 "제품 추천받기" 버튼이 호출한다.

1. 고정 질문("내 영양소 목표에 맞는 제품을 추천해줘")을 사용자 메시지로 저장한다(대화 이력에 남는다). 고정 문구이므로 안전 분류는 생략한다.
2. 스냅샷을 만든 뒤 결정적으로 분기한다. 영양소 목표가 없으면 목표 등록 안내를, 목표는 있지만 매칭 제품이 없으면 매칭 없음 안내를 LLM 호출 없이 저장·반환한다.
3. 후보가 있으면 추천 전용 근거(`ChatReferenceCatalog.productRecommendation()`)로 OpenAI를 호출한다. 근거는 후보 순서 유지, 매칭 영양소·기능정보 문구 그대로 사용, 미포함 제품·복용법 언급 금지, 건강기능식품 고지를 강제한다.
4. 응답 스키마는 일반 채팅과 동일한 `ChatSendResponse`이며 `context.nutritionRecommendations`에 후보가 담겨 프론트가 제품 카드를 함께 렌더링할 수 있다.

## 영양 API

기존 `nutrition_contents`에 API 명세가 요구하는 `content_type`, `timing_tag`, `thumbnail_url`, `source_url`, `disclaimer`를 보강한다.

- `GET /nutrition-contents`
- `GET /nutrition-contents/{contentId}`
- `POST /nutrition-contents/{contentId}/favorite`
- `DELETE /nutrition-contents/{contentId}/favorite`
- `GET /users/me/favorites`

`timingTag` 미지정 시 근무표와 현재 시각으로 백엔드가 추천 태그를 결정한다. 콘텐츠는 제공된 두 기준 문서의 영양 근거 범위를 벗어나는 개인 진단·효능 단정을 하지 않는다.

## 영양소 목표 기반 제품 추천

### 안전 경계

v2 문서가 명시하듯 현재 데이터만으로 개인의 영양 결핍을 측정하지 않는다. 챗봇은 피로, 불면, 피부 상태 같은 증상에서 부족 영양소를 추론하지 않는다. 추천 입력은 사용자가 직접 등록했거나 건강검진·전문가 상담에서 확인했다고 표시한 영양소 목표만 사용한다.

- `USER_REPORTED`: 사용자가 보충 관심 영양소로 직접 등록
- `HEALTH_CHECK`: 사용자가 건강검진 등에서 확인했다고 등록

두 값 모두 서비스가 의학적으로 검증했다는 의미가 아니다. 복용 중인 약, 임신·수유, 알레르기, 질환 관련 질문은 기존 `MEDICAL_REFERRAL` 경계를 적용한다.

건강기능식품은 의약품이 아니며 질병 예방·치료 효과를 표현하지 않는다. 제품 기능정보는 제공된 표시 문구를 그대로 저장하고, 출시 전 실제 제품 라벨과 광고심의 결과를 운영자가 재확인해야 한다.

### 데이터와 매칭

- `nutrition_products`: 제품명, 영문명, 공통 고지문
- `nutrition_product_functions`: 기능성 원료, 표시 기능정보, 매칭 가능한 표준 영양소 코드
- `user_nutrient_needs`: 사용자 영양소 코드, 입력 출처, 확인일

추천 후보는 `user_nutrient_needs.nutrient_code`와 제품 기능정보의 `nutrient_code` 교집합 개수로만 정렬한다. 같은 점수는 제품 ID 순으로 고정한다. LLM은 후보 추가·삭제·재정렬, 결핍 판정, 용량·복용법 계산을 하지 않는다. 락티움·테아닌·콜라겐펩타이드·히알루론산·밀크씨슬·코엔자임Q10 같은 기능성 원료는 제품 정보에는 노출하지만 영양소 부족 매칭 키로 사용하지 않는다.

### API와 챗봇

- `PUT /users/me/nutrient-needs`: 영양소 목표 전체 교체
- `GET /users/me/nutrient-needs`: 현재 목표 조회
- `DELETE /users/me/nutrient-needs/{nutrientCode}`: 단일 삭제
- `GET /nutrition-products`: 제품과 표시 기능정보 조회
- `GET /users/me/nutrition-product-recommendations`: 백엔드 매칭 결과 조회

`ChatContextSnapshot`에는 영양소 목표와 결정적으로 계산된 제품 후보만 주입한다. `NUTRITION` 질문의 프롬프트에는 “제품명·기능정보·매칭 영양소를 주어진 값 그대로 설명하고, 치료·예방·결핍 진단·복용량을 만들지 않는다”는 규칙을 추가한다.

## 오류와 트랜잭션

- 기록 CRUD는 사용자 소유권을 서비스 계층에서 검사한다.
- Chat 사용자·응답 저장은 한 트랜잭션으로 처리한다. 모델 실패 시 질문 저장도 롤백하고 503을 반환한다.
- 모델 응답이 스키마를 위반하거나 비어 있으면 저장하지 않고 503으로 처리한다.
- 응급 분기는 모델 상태와 무관하게 고정 안전 응답을 반환한다.
- 로그 삭제·수정 후 기존 코칭을 즉시 대량 재생성하지 않고, 다음 조회 또는 명시적 재생성 시 최신 기록을 사용한다.

## 테스트 전략

- 마이그레이션: 실제 MySQL Testcontainers로 신규 테이블과 제약 검증
- 도메인: 수면 겹침, 시각 순서, 카페인 양수 검증
- 서비스: 소유권, 기간 조회, 합계, 수정·삭제
- 계산기: 자정 경계 원형 평균, 로그 1건, 로그 없음, 잔 수 경고, 폴백
- Chat: 시스템 규칙, 도메인 발췌, 스냅샷 누락값, structured output 매핑, 안전 분기, 모델 실패
- Nutrition: 필터·자동 timing tag·페이징·즐겨찾기
- 통합: JWT를 포함한 HTTP 계약과 DB 저장 흐름
- 회귀: 전체 Gradle 테스트와 Docker 환경 기동 확인

## 제외 범위

- 웨어러블 기기 실제 연동
- 유전자 기반 카페인 반감기 추정
- 근거가 확보되지 않은 낮잠 수면압력 임계치
- 증상 기반 영양 결핍 진단, 개인 맞춤 처방·복용량 및 정밀 식단
- OCR API 경로 개편
