# ShiftRhythm Backend

교대근무 리듬 코칭 앱의 REST API 서버입니다. Java 21, Spring Boot 4.1, MySQL, Redis를 사용합니다.

## 사전 요구사항

- 실행 중인 Docker Desktop (Docker Compose v2 포함)
- Java 21 (컨테이너 밖에서 Gradle 테스트를 실행할 경우)
- gcloud CLI + ADC 로그인 (OCR 일정 입력 기능 사용 시):

```bash
gcloud auth application-default login --impersonate-service-account=shiftmate-vision@shiftmate-504210.iam.gserviceaccount.com
```

키 파일 없이 서비스 계정 impersonation으로 Document AI를 호출합니다. Linux/macOS에서는 `.env`에 `GCLOUD_CONFIG_DIR=~/.config/gcloud`를 지정하세요 (Windows는 기본값 `%APPDATA%/gcloud` 사용).

통합 테스트는 Testcontainers로 MySQL·Redis 컨테이너를 띄우므로, 테스트를 실행할 때도 Docker가 실행 중이어야 합니다.

## 로컬 실행

1. `.env.example`을 복사해 `.env` 파일을 만듭니다.
2. `.env`의 `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `JWT_SECRET`, `GOOGLE_CLIENT_ID`를 실제 로컬 값으로 교체합니다.
3. 다음 명령을 실행합니다.

```bash
docker compose up --build
```

API는 `http://localhost:8080`에서 실행됩니다. MySQL과 Redis 데이터는 Docker 볼륨에 보존됩니다.
호스트의 MySQL 포트는 기존 로컬 MySQL과 충돌하지 않도록 기본 `3307`을 사용합니다.

`local` 프로파일에서는 인증 필터가 비활성화되고 고정 사용자(`id=1`) 기준으로 동작합니다.

## 환경변수

| 변수 | 설명 |
| --- | --- |
| `MYSQL_DATABASE` | MySQL 데이터베이스 이름 |
| `MYSQL_USER` / `MYSQL_PASSWORD` | 애플리케이션 MySQL 계정 |
| `MYSQL_ROOT_PASSWORD` | MySQL root 비밀번호 |
| `JWT_SECRET` | 32바이트 이상을 Base64로 인코딩한 JWT 서명 키 |
| `GOOGLE_CLIENT_ID` | Google OAuth 웹 클라이언트 ID |
| `JWT_ISSUER` | JWT issuer 값 |
| `JWT_ACCESS_TOKEN_EXPIRATION` | 액세스 토큰 만료 기간 (기본 `PT30M`) |
| `JWT_REFRESH_TOKEN_EXPIRATION` | 리프레시 토큰 만료 기간 (기본 `P30D`) |
| `DOCUMENTAI_PROJECT_NUMBER` | Document AI GCP 프로젝트 번호 (기본값: 팀 공용) |
| `DOCUMENTAI_LOCATION` | Document AI 프로세서 리전 (기본 `us`) |
| `DOCUMENTAI_PROCESSOR_ID` | Document AI Form Parser 프로세서 ID (기본값: 팀 공용) |
| `GCLOUD_CONFIG_DIR` | 컨테이너에 마운트할 호스트 gcloud 설정 디렉터리 |

`.env` 파일은 Git에서 제외됩니다. 비밀값을 저장소에 커밋하지 마세요.

## 빌드 / 테스트

```bash
./gradlew test              # 전체 테스트 (Docker 필요)
./gradlew bootJar -x test   # JAR 빌드
```

Windows에서는 `gradlew.bat`을 사용합니다.

Compose 설정만 확인하려면:

```bash
docker compose --env-file .env.example config
```

## API 개요

Base URL은 `/api/v1`이며, `/api/v1/auth/**`를 제외한 모든 경로는 인증이 필요합니다.
오류 응답은 `{ "message": "..." }` 형식을 사용합니다.

| 도메인 | 엔드포인트 |
| --- | --- |
| 인증 | `POST /auth/google`, `POST /auth/reissue`, `POST /auth/logout` |
| 내 정보 | `GET·PATCH·DELETE /users/me` |
| 개인화 설정 | `GET·PUT /users/me/settings` |
| 알림 설정 | `GET·PUT /users/me/notification-settings` |
| 근무 일정 | `POST·GET /shifts`, `PATCH·DELETE /shifts/{shiftId}`, `PATCH /shifts:bulk`, `POST /shifts/pattern`, `GET /shifts/completeness` |
| 반복 패턴 | `GET·POST /shift-patterns`, `DELETE /shift-patterns/{patternId}` |
| 코칭 | `GET /coachings`, `GET /coachings/cards/{cardId}`, `POST /coachings:regenerate` |
| 전환 프로토콜 | `GET /transitions`, `GET /transitions/{date}` |
| 홈 | `GET /home/dashboard` |
| 루틴 | `GET /routines`, `GET /routines/summary`, `GET /routines/report`, `PATCH /routines/tasks/{taskId}` |
| 낮잠 | `POST /naps`, `GET /naps/active`, `PATCH /naps/{napId}` |
| OCR 일정 입력 | `POST /ocr/jobs`, `GET /ocr/jobs/{jobId}`, `PATCH /ocr/jobs/{jobId}/drafts/{draftId}`, `POST /ocr/jobs/{jobId}:confirm`, `POST /ocr/jobs/{jobId}:retry` |

일정을 변경하는 API(`PATCH /shifts/{shiftId}`, `PATCH /shifts:bulk`, `POST /shifts/pattern`)는 영향 범위의 코칭을 자동으로 재생성하고, 응답에 `affectedCoachingDates`를 포함합니다.

## 인증

Google ID Token을 검증한 뒤 자체 JWT 액세스·리프레시 토큰을 발급합니다. 리프레시 토큰은 Redis에 저장합니다.

로그아웃하면 리프레시 토큰을 삭제하고, 해당 사용자에게 이미 발급된 액세스 토큰도 만료 전까지 사용할 수 없도록 무효화합니다. 이후 토큰을 재발급받으면 다시 정상적으로 사용할 수 있습니다.

## 구현 현황

- **P0 플랫폼 기반** — Docker Compose, Flyway(V1~V6), 공통 예외 처리, 보안 필터 체인
- **P1 인증·사용자** — Google 로그인, 토큰 재발급·로그아웃, 내 정보, 개인화·알림 설정
- **P2 근무 일정** — 일정 CRUD, 기간 조회, 4주 충족도, 일괄 변경, 반복 패턴
- **P3 코칭·전환·홈** — 코칭 카드 생성·재생성, 전환 프로토콜 가이드, 홈 대시보드
- **P4 루틴·낮잠** — 루틴 완료/건너뛰기, 스트릭·리포트, 낮잠 타이머 (코칭 연동은 미완)
- **P5 OCR 일정 입력** — 이미지 업로드, Document AI 비동기 분석, 초안 검수·보정·확정, 재시도

P6(AI 채팅·영양 콘텐츠)는 아직 구현되지 않았습니다.

## 참고 문서

- `docs/backend-priority.md` — 우선순위(P0~P6)와 공통 API 규칙
- `.env.example` — 로컬 환경변수 템플릿
