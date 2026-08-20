# 시연 환경 세팅 가이드

QA에서 올라온 이슈(개발용 인증 우회, 업로드 500, 시연 데이터 부재)를 반영한 뒤
서버에서 실행해야 하는 작업을 순서대로 정리한 문서다.

---

## 1. 배포 프로파일 변경 (인증 우회 제거)

기존 배포는 `local` 프로파일로 떠 있었다. `local` 프로파일에는
**JWT 필터가 아예 등록되지 않고 모든 요청이 `permitAll`** 이며, 현재 사용자는 항상
`local@shiftrhythm.test`(user id 1)로 고정된다. `Authorization` 헤더 없이 `/api/v1/users/me`가
200을 반환한 원인이 이것이다.

이번 변경으로:

- `application.yml`의 기본 프로파일이 `local` → **`prod`** 로 바뀌었다.
  프로파일을 지정하지 않고 띄우면 이제 인증이 켜진 상태로 뜬다.
- DB·Redis 접속 설정이 `application-local.yml`에만 있었는데 `application.yml`(공통)로 옮겼다.
  이 작업 없이 프로파일만 바꾸면 datasource가 없어서 부팅에 실패한다.
- `docker-compose.yml`은 `SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-local}`로 바꿔서
  로컬 개발은 그대로 `local`, 서버는 환경변수로 덮어쓸 수 있게 했다.

### 서버에서 할 일

```bash
# 배포 환경변수에 아래를 추가 (또는 아예 지정하지 않아도 prod로 뜬다)
SPRING_PROFILES_ACTIVE=prod
```

### 로컬 개발자가 할 일

`bootRun`이나 IDE로 직접 띄울 때는 프로파일을 명시해야 기존처럼 인증 없이 개발할 수 있다.

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

`docker compose up`은 기본값이 `local`이라 그대로 두면 된다.

### 확인 방법

```bash
curl -i https://<서버>/api/v1/users/me
# → 401, {"message":"로그인이 필요합니다."}

curl -i -H "Authorization: Bearer <만료된 토큰>" https://<서버>/api/v1/users/me
# → 401, 동일한 JSON 스키마
```

---

## 2. 시연 계정 데이터 시드

### 실행 순서

1. **시연 계정으로 앱에서 구글 로그인을 먼저 한 번 한다.**
   이 시점에 `users` 행이 생성된다. 로그인 없이 스크립트를 돌리면 `user_id`가 NULL이라 실패한다.
2. `docs/demo-seed-data.sql`을 열어 맨 위 `@demo_email`을 시연 계정 이메일로 바꾸고,
   시연 시각에 맞춰 `@cycle_offset`을 고른다 (아래 표 참고).
3. 서버 DB에 실행한다.

   ```bash
   mysql -h <MYSQL_HOST> -u <MYSQL_USER> -p shiftrhythm < docs/demo-seed-data.sql
   ```

4. 스크립트 마지막의 확인 SELECT 결과를 본다.
   - `앞으로 28일 확정 일수` = **28**
   - `오늘 근무 (EVENING) - OFF 아니어야 코칭 200` = **1**  (괄호 안은 오늘 근무 유형)

> ⚠️ 이 스크립트는 `@demo_email` 계정의 근무표·코칭·루틴·수면·카페인·낮잠 데이터를
> **지우고 다시 넣는다.** 실제 사용자 계정에는 실행하지 말 것.
> 날짜는 실행 시점의 `CURDATE()` 기준이라 시연 당일에 실행하는 게 가장 정확하다.

### 시연 시각에 맞춰 `@cycle_offset` 고르기

스크립트 상단의 `SET @cycle_offset = 0;` 이 **오늘 근무 유형**을 정한다.

| 값 | 오늘 근무 | 코칭 카드 창 | 언제 쓰나 |
|---|---|---|---|
| `0` | DAY | 03:00 ~ 11:00 | 오전 시연 |
| `2` | EVENING | 11:00 ~ 19:00 | 오후 시연 |
| `4` | NIGHT | 19:00 ~ 24:00 | 저녁·야간 시연 |

홈 대시보드의 `topCoachingCards`는 **아직 끝나지 않은 카드 창만** 내려준다.
오후 3시에 시연하는데 `@cycle_offset = 0`(DAY, 마지막 창이 11:00 종료)이면
`topCoachingCards`가 `[]`로 비어 보인다. 버그가 아니라 설계된 동작이다.

`GET /api/v1/coachings`는 창이 지났어도 카드를 그대로 내려주므로, 비는 건 홈 상단 요약뿐이다.
어느 값을 골라도 **오늘은 항상 전환일**이라 전환일 화면은 영향을 받지 않는다.

### 넣어지는 데이터

| 항목 | 범위 | 비고 |
|---|---|---|
| 근무표 | 오늘 -14일 ~ +27일 (42일) | 7일 주기 `D-D-E-E-N-N-OFF` |
| 수면 기록 | 최근 14일 | 근무 유형별로 취침 시각이 다름 |
| 카페인 기록 | 최근 7일 × 2건 | 아메리카노 150mg, 아이스티 75mg |
| 낮잠 기록 | 최근 3일 | 전부 `COMPLETED` |

앞으로 28일이 빈틈없이 차 있으므로 `/api/v1/shifts/completeness?weeks=4`가 100%가 된다.
근무 주기가 `D→E→N`으로 돌기 때문에 전환일이 자연스럽게 생기고,
**직전 근무일과 오늘의 근무 유형이 달라 오늘 자체가 전환일**이다 (`@cycle_offset` 값과 무관).

### 진행 중 낮잠 세션은 일부러 넣지 않았다

시연 때 `POST /api/v1/naps`로 직접 타이머를 시작해야 낮잠 타이머가 도는 걸 보여줄 수 있고,
`RUNNING` 세션이 미리 있으면 새로 시작하는 게 막힌다.

---

## 3. 코칭 카드 · 루틴 항목 생성 (SQL 실행 후 필수)

코칭 카드와 루틴 항목은 **SQL로 넣지 않는다.** 근무표를 기준으로 서버가 생성하는 값이라
직접 INSERT하면 실제 로직과 어긋난다. 대신 시드 실행 후 재생성 API를 한 번 호출한다.

```bash
curl -X POST https://<서버>/api/v1/coachings:regenerate \
  -H "Authorization: Bearer <시연 계정 액세스 토큰>" \
  -H "Content-Type: application/json" \
  -d '{"from":"<오늘-14일>","to":"<오늘+27일>"}'
```

응답의 `regeneratedCount`가 0이 아니면 성공이다. (OFF인 날은 건너뛰므로 42보다 작다.)
이 한 번의 호출로 `daily_coachings`, `coaching_cards`, `routine_tasks`가 전부 채워진다.

재생성 범위는 최대 90일까지 허용된다.

### 시연 직전 체크리스트

```bash
GET /api/v1/coachings                      # 200, 카드 목록 있음
GET /api/v1/routines                       # 루틴 항목 있음
GET /api/v1/transitions?from=&to=          # 전환일 목록 있음
GET /api/v1/shifts/completeness?weeks=4    # 100%
GET /api/v1/home/dashboard                 # 홈 화면 데이터
```

---

## 4. `/api/v1/coachings`가 409를 반환할 때

`409 근무 일정을 먼저 등록해 주세요.`는 **28일을 못 채워서가 아니다.**

`CoachingService`는 조회 날짜(기본값: 오늘) 하루치 근무 레코드가 없으면 409를 던진다.
28일 규칙은 `/api/v1/shifts/completeness` 전용이고 코칭과 연결돼 있지 않다.

- 오늘 근무가 1건이라도 있으면 → 200
- 오늘 근무가 없으면 → 며칠치를 등록했든 409
- 오늘이 `OFF`인 경우에도 레코드 자체는 있으므로 200

즉 "28일을 채우면 해결된다"는 건 그 범위에 오늘이 포함돼서 맞는 말이고,
정확한 조건은 **조회 날짜의 근무 레코드 존재 여부**다.
시드 스크립트는 오늘을 항상 DAY 근무로 만들어 두므로 이 문제가 생기지 않는다.
