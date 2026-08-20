# 실제 API 라우트 목록

프론트에서 호출 중인 경로와 서버 구현이 어긋나 404가 나는 건이 있어, 컨트롤러 매핑을 그대로 옮겨 적은 문서다.
모든 경로에 `SERVER_CONTEXT_PATH`(기본값 없음)가 앞에 붙는다.

## 보고된 경로 불일치

| 호출하던 경로 | 실제 경로 |
|---|---|
| `POST /api/v1/schedule-uploads` | **`POST /api/v1/ocr/jobs`** |
| `POST /auth/google` | **`POST /api/v1/auth/google`** |
| `POST /auth/reissue` | **`POST /api/v1/auth/reissue`** |
| `POST /auth/logout` | **`POST /api/v1/auth/logout`** |

`schedule-uploads`라는 이름은 백엔드에 존재한 적이 없다. 근무표 이미지 업로드는 처음부터 OCR 잡 생성 엔드포인트다.

### 근무표 이미지 업로드

```
POST /api/v1/ocr/jobs
Content-Type: multipart/form-data

image  (필수)  JPEG 또는 PNG, 최대 10MB
month  (선택)  yyyy-MM   ※ 이미지에 "2025년 06월" 같은 연월 표기가 있으면 그쪽이 우선한다

→ 202 Accepted  {"jobId": 1, "status": "PENDING"}
```

분석은 비동기라 202를 받은 뒤 `GET /api/v1/ocr/jobs/{jobId}`로 폴링해서
`status`가 `COMPLETED`가 되면 `drafts`를 검수하고 `:confirm`으로 확정한다.

### 검수 단계에서 초안 손보기

OCR이 근무 유형으로 읽지 못한 칸(예: "교육" 배지)은 초안이 아예 만들어지지 않는다.
이 경우 수정할 대상이 없으므로 확정 전에 직접 추가한다.

```
POST /api/v1/ocr/jobs/{jobId}/drafts
Content-Type: application/json

{"workDate": "2025-06-10", "shiftType": "DAY", "startTime": "09:00", "endTime": "18:00"}
   workDate   (필수)  yyyy-MM-dd, 잡의 대상 월 안이어야 한다
   shiftType  (필수)  DAY | EVENING | NIGHT | OFF
   startTime / endTime  (선택)  HH:mm

→ 201 Created  {"draftId": 7, "workDate": "2025-06-10", ...}
→ 400          날짜가 대상 월을 벗어남
→ 409          잡이 COMPLETED 상태가 아님
```

직접 넣은 초안은 신뢰도가 최대값이라, 같은 날짜에 인식된 초안이 있으면 그쪽을 밀어내고
확정된다. 밀려난 날짜는 `:confirm` 응답의 `skippedDates`로 알려준다.

## 인증

| 메서드 | 경로 |
|---|---|
| POST | `/api/v1/auth/google` |
| POST | `/api/v1/auth/reissue` |
| POST | `/api/v1/auth/logout` |

`/api/v1/auth/**`만 비인증 허용이고, 나머지 `/api/v1/**`는 전부 인증이 필요하다.

## 전체 목록

| 메서드 | 경로 |
|---|---|
| GET / PATCH / DELETE | `/api/v1/users/me` |
| GET / PUT | `/api/v1/users/me/settings` |
| GET / PUT | `/api/v1/users/me/notification-settings` |
| GET / PUT | `/api/v1/users/me/nutrient-needs` |
| DELETE | `/api/v1/users/me/nutrient-needs/{nutrientCode}` |
| GET | `/api/v1/users/me/favorites` |
| GET | `/api/v1/users/me/nutrition-product-recommendations` |
| GET / PUT | `/api/v1/users/me/shift-time-defaults` |
| GET | `/api/v1/home/dashboard` |
| POST / GET | `/api/v1/shifts` |
| PATCH / DELETE | `/api/v1/shifts/{shiftId}` |
| PATCH | `/api/v1/shifts:bulk` |
| POST | `/api/v1/shifts/pattern` |
| GET | `/api/v1/shifts/completeness` |
| GET / POST | `/api/v1/shift-patterns` |
| DELETE | `/api/v1/shift-patterns/{patternId}` |
| POST | `/api/v1/ocr/jobs` |
| GET | `/api/v1/ocr/jobs/{jobId}` |
| POST | `/api/v1/ocr/jobs/{jobId}/drafts` |
| PATCH | `/api/v1/ocr/jobs/{jobId}/drafts/{draftId}` |
| POST | `/api/v1/ocr/jobs/{jobId}:confirm` |
| POST | `/api/v1/ocr/jobs/{jobId}:retry` |
| GET | `/api/v1/coachings` |
| GET | `/api/v1/coachings/cards/{cardId}` |
| POST | `/api/v1/coachings:regenerate` |
| GET | `/api/v1/routines` |
| GET | `/api/v1/routines/summary` |
| GET | `/api/v1/routines/report` |
| PATCH | `/api/v1/routines/tasks/{taskId}` |
| GET | `/api/v1/transitions` |
| GET | `/api/v1/transitions/{date}` |
| GET | `/api/v1/naps/active` |
| POST | `/api/v1/naps` |
| PATCH | `/api/v1/naps/{napId}` |
| POST / GET | `/api/v1/sleep-logs` |
| PATCH / DELETE | `/api/v1/sleep-logs/{sleepLogId}` |
| POST / GET | `/api/v1/caffeine-intakes` |
| PATCH / DELETE | `/api/v1/caffeine-intakes/{intakeId}` |
| POST / GET | `/api/v1/chat/messages` |
| POST | `/api/v1/chat/messages:recommend-products` |
| POST | `/api/v1/chat/messages/{messageId}/feedback` |
| GET | `/api/v1/nutrition-contents` |
| GET | `/api/v1/nutrition-contents/{contentId}` |
| POST / DELETE | `/api/v1/nutrition-contents/{contentId}/favorite` |
| GET | `/api/v1/nutrition-products` |

## 에러 응답 규약

모든 에러는 동일한 스키마다.

```json
{"message": "사람이 읽을 수 있는 한국어 메시지"}
```

프레임워크 레벨 오류가 전부 500으로 뭉개지던 문제를 고쳐서, 이제 아래처럼 내려간다.

| 상황 | 상태 코드 | 메시지 |
|---|---|---|
| 필수 multipart 파트 누락 | 400 | `image 항목은 필수입니다.` |
| 필수 쿼리 파라미터 누락 | 400 | `to 파라미터는 필수입니다.` |
| 파라미터 타입 불일치 | 400 | `from 값이 올바르지 않습니다.` |
| 요청 본문 JSON 파싱 실패 | 400 | `요청 본문을 읽을 수 없습니다.` |
| 잘못된 multipart 요청 | 400 | `멀티파트 요청 형식이 올바르지 않습니다.` |
| 인증 없음 / 토큰 만료 | 401 | `로그인이 필요합니다.` |
| 없는 경로 | 404 | `요청한 리소스를 찾을 수 없습니다.` |
| 지원하지 않는 메서드 | 405 | `지원하지 않는 HTTP 메서드입니다.` |
| 지원하지 않는 Content-Type | 415 | `지원하지 않는 Content-Type입니다.` |
| 업로드 용량 초과 | 413 | `업로드할 수 있는 최대 용량을 초과했습니다.` |
| 그 외 예기치 못한 오류 | 500 | `서버 오류가 발생했습니다.` |

도메인 규칙 위반(중복 근무, 확정 불가 상태 등)은 각 도메인 예외의 메시지가 그대로 내려간다.
