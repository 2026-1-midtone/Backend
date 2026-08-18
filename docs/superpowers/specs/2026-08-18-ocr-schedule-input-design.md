# P5 — OCR 일정 입력 설계

2026-08-18 승인. 근무표 이미지를 업로드하면 Document AI Form Parser로 표를 인식해 일정 초안을 만들고, 사용자가 검수·보정한 뒤 확정하면 실제 근무 일정에 반영한다.

## 결정 사항

| 항목 | 결정 | 근거 |
| --- | --- | --- |
| OCR 엔진 | Google Document AI **Form Parser** | 근무표는 표 형태이므로 행·열·셀로 분해된 결과를 그대로 사용. Vision은 좌표→표 복원 휴리스틱을 직접 작성해야 함 |
| 인증 | ADC + 서비스 계정 impersonation (`shiftmate-vision@shiftmate-504210.iam.gserviceaccount.com`) | 조직 정책이 SA 키 파일 생성을 금지. impersonation은 별개 권한으로 허용됨. 키 파일 배포 불필요 |
| 프로세서 | `projects/437332095325/locations/us/processors/cbb47f2525db9dc0` (Form_Parser, ENABLED) | 이미 생성되어 있음 |
| 이미지 보관 | MySQL `MEDIUMBLOB` | 유저당 월 1~2회 업로드 규모. 인프라 추가 없음. 확정 시 BLOB 비움 |
| 비동기 실행 | Spring `@Async` 스레드풀 | Document AI 온라인 처리는 수 초 내. 서버 재시작으로 잡이 유실되면 FAILED 처리 후 재시도 API로 복구 |
| 확정 충돌 | 기존 일정 **덮어쓰기** | 사용자가 검수 화면에서 확인한 데이터가 최종본. 응답에 `replacedDates` 포함 |
| HTTP 클라이언트 | Spring `RestClient` + `google-auth-library-oauth2-http` | 무거운 gRPC SDK 대신 REST 직접 호출. ADC에서 Bearer 토큰 발급 |

## API 계약

Base URL `/api/v1`. 오류 응답 `{ "message": "..." }`, 상태 코드 규칙(400/401/403/404/409)은 기존과 동일. 소유권 검사는 서비스 계층에서 수행한다.

| 메서드 | 경로 | 동작 |
| --- | --- | --- |
| `POST /ocr/jobs` | multipart 이미지 업로드. 잡 생성(PENDING) 후 `202 { jobId, status }`. @Async로 처리 시작 |
| `GET /ocr/jobs/{jobId}` | 상태 조회. `PENDING → PROCESSING → COMPLETED / FAILED`, 확정 후 `CONFIRMED`. COMPLETED면 초안 목록 포함 |
| `PATCH /ocr/jobs/{jobId}/drafts/{draftId}` | 초안 보정: 날짜·근무유형·시간 수정, `excluded`로 항목 제외 |
| `POST /ocr/jobs/{jobId}:confirm` | 초안(excluded 제외)을 `shift_schedules`에 반영(source=OCR, confirmed=true). 겹치는 날짜는 덮어쓰기. 응답에 `replacedDates`, `affectedCoachingDates`. 잡은 CONFIRMED로 종결, 이미지 BLOB 비움 |
| `POST /ocr/jobs/{jobId}:retry` | FAILED 또는 COMPLETED 잡을 보관된 원본 이미지로 재처리 |

검증 규칙:

- 이미지는 JPEG/PNG만 허용, 최대 10MB. 위반 시 400.
- 보정·확정은 COMPLETED 상태에서만, 재시도는 FAILED·COMPLETED에서만 가능. 위반 시 409.
- 다른 사용자의 잡 접근은 403, 없는 잡·초안은 404.

## 데이터 모델 — V7 마이그레이션

```sql
ocr_jobs
  id BIGINT PK AUTO_INCREMENT
  user_id BIGINT NOT NULL, FK users ON DELETE CASCADE
  status VARCHAR(20) NOT NULL            -- PENDING/PROCESSING/COMPLETED/FAILED/CONFIRMED
  image MEDIUMBLOB                       -- 확정 시 NULL로 비움
  image_mime VARCHAR(30)
  error_message VARCHAR(255)             -- FAILED 사유
  created_at, updated_at DATETIME(6)

ocr_draft_shifts
  id BIGINT PK AUTO_INCREMENT
  job_id BIGINT NOT NULL, FK ocr_jobs ON DELETE CASCADE
  work_date DATE NOT NULL
  shift_type VARCHAR(20) NOT NULL
  start_time TIME, end_time TIME
  confidence DECIMAL(4,3)
  excluded BOOLEAN NOT NULL DEFAULT FALSE
  UNIQUE (job_id, work_date)
```

## 컴포넌트 — `ocr` 패키지

기존 컨벤션(도메인 루트 Controller, `application`/`domain` 하위 패키지)을 따른다.

```
ocr/
  OcrController.java
  application/
    OcrJobService.java          -- 업로드·상태 조회·보정·확정·재시도
    OcrProcessingWorker.java    -- @Async: Document AI 호출 → 초안 저장 → 상태 갱신
    OcrDraftParser.java         -- Form Parser 표 응답 → (날짜, ShiftType, confidence) 초안 변환
    OcrException.java
  domain/
    OcrJob.java, OcrJobStatus.java, OcrJobRepository.java
    OcrDraftShift.java, OcrDraftShiftRepository.java
  documentai/
    DocumentAiClient.java       -- RestClient + ADC Bearer 토큰으로 :process 호출
```

파싱 규칙:

- Form Parser가 반환한 표 셀에서 날짜 열/헤더를 찾아 각 행을 날짜에 대응시킨다.
- 셀 텍스트는 내장 사전으로 매핑한다: `D/DAY/데이 → DAY`, `E/EVE/이브닝 → EVENING`, `N/NIGHT/나이트 → NIGHT`, `OFF/휴/X → OFF`.
- 매핑 실패 셀은 초안에서 제외하고, Document AI confidence를 초안에 기록한다.
- 시작·종료 시간은 사용자 개인화 설정의 근무유형별 기본 시간을 적용한다(수동 입력과 동일한 규칙).

확정 흐름:

1. excluded가 아닌 초안을 날짜순으로 `shift_schedules`에 반영한다. 같은 날짜의 기존 일정은 삭제 후 재생성(source=OCR, confirmed=true).
2. 기존 일정 변경 API와 동일하게 `ShiftCoachingRegenerationTrigger`로 영향 범위 코칭을 재생성한다.
3. 잡을 CONFIRMED로 바꾸고 이미지 BLOB을 비운다.

## 설정

- `application.yml`: `app.documentai.project-number`, `app.documentai.location`, `app.documentai.processor-id` (환경변수 `DOCUMENTAI_*` 주입, 기본값은 위 프로세서).
- `docker-compose.yml`: 호스트 gcloud 설정 디렉터리를 컨테이너 `/root/.config/gcloud`에 read-only 마운트. `GOOGLE_CLOUD_PROJECT=shiftmate-504210` 환경변수 추가.
- 로컬 개발 사전 조건: `gcloud auth application-default login --impersonate-service-account=shiftmate-vision@shiftmate-504210.iam.gserviceaccount.com` 1회 실행.
- 배포가 GCP(Cloud Run/GCE)로 갈 경우 서비스 계정을 직접 붙이면 impersonation 없이 동작한다.

## 오류 처리

- Document AI 호출 실패(네트워크·4xx·5xx): 잡을 FAILED로 바꾸고 `error_message`에 사유 기록. 사용자는 재시도 API로 복구.
- 파싱 결과 초안 0건: FAILED 처리("근무표를 인식하지 못했습니다").
- 서버 재시작으로 PROCESSING에 멈춘 잡: 앱 기동 시 PROCESSING 잡을 FAILED로 정리한다.

## 테스트

- `OcrDraftParser`: 실제 Form Parser 응답 JSON 픽스처 기반 단위 테스트 (날짜 매핑, 사전 매핑, 실패 셀 제외).
- `DocumentAiClient`: `MockRestServiceServer`로 응답 고정 — 실제 GCP 불필요.
- `OcrJobService`: 상태 전이·소유권·검증 단위 테스트.
- 업로드→상태조회→보정→확정(덮어쓰기·코칭 재생성 포함) 흐름은 Testcontainers 통합 테스트.

## 남은 외부 의존

- **프로젝트 결제 계정 연결이 아직 안 됨** (`BILLING_DISABLED`). 코드 개발·테스트는 목 기반으로 진행 가능하나, 실제 이미지 E2E 검증 전에 콘솔에서 연결 필요: https://console.cloud.google.com/billing/linkedaccount?project=shiftmate-504210
