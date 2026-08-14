# ShiftRhythm Backend

Spring Boot 4.1 / Java 21 기반 백엔드. 교대근무자 생활 리듬 코칭 앱의 API 서버입니다.

## 아키텍처 규칙

도메인별로 패키지를 나누고, 각 도메인 안에서는 역할별로 계층을 나눕니다. (`nap`, `routine` 참고)

```
com.midtone.backend.{도메인}/
  {도메인}Controller.java      # 최상위: HTTP 요청/응답만 담당
  application/                 # 비즈니스 로직 (서비스)
  domain/                      # 엔티티, 리포지토리, 도메인 규칙
```

- **Controller**: 요청 파싱, DTO 변환, 서비스 호출, 응답 반환만 한다. 비즈니스 로직을 컨트롤러에 넣지 않는다.
- **Application(Service)**: 실제 유스케이스 로직이 여기 있다. 트랜잭션 경계도 여기서 관리한다(`@Transactional`). 유스케이스 자체와 그 요청/응답 DTO만 둔다.
- **Domain**: JPA 엔티티, `Repository` 인터페이스, 엔티티 자체가 가져야 할 규칙(불변식 검증 등)만 둔다. 프레임워크 의존성(컨트롤러, 서비스)을 domain 패키지가 알게 하지 않는다.
- 공통 기능(에러 응답, 인증 사용자 조회 등)은 `global` 패키지에 둔다. (`global/error`, `global/user`, `global/config` 참고)
- `application`/`domain`만으로는 관심사가 뒤섞이는 경우(외부 연동, 토큰 처리 등 기술적으로 독립된 덩어리), 도메인 패키지 밑에 그 관심사 이름으로 하위 패키지를 추가로 둔다. (`auth/jwt`, `auth/google` 참고 — JWT 발급/검증과 구글 토큰 검증은 서로 변경 이유가 다르고, 유스케이스 로직도 아니라서 `application`에서 분리했다.) 하위 패키지가 하나 더 생기는 것 자체보다, `application` 폴더 하나에 성격이 다른 클래스가 뒤섞이는 걸 더 피한다.
- 한 도메인이 서로 다른 REST 리소스(유스케이스)를 여러 개 가지면서 `application` 파일 수가 많아지면(대략 10개 이상), 유스케이스 이름으로 `application` 밑에 하위 패키지를 나눈다. (`user/application/profile`, `user/application/settings`, `user/application/notification` 참고 — 내 정보, 개인화 설정, 알림 설정은 각자 컨트롤러·서비스·DTO가 따로 있고 서로 의존하지 않아서 나눴다.) 이건 `auth/jwt`처럼 "기술적으로 독립된 덩어리"를 분리하는 것과는 다른 이유다 — 여기서는 세 유스케이스 모두 성격이 같은 CRUD라서 `domain`까지 나눌 필요는 없고(엔티티들이 서로 가볍고 자주 참조되므로 `domain`은 그대로 flat하게 둔다), `application`만 유스케이스 단위로 쪼갠다.

## 코드 컨벤션

### 네이밍
- 클래스: `PascalCase`, 역할이 드러나는 이름 (`JwtProvider`, `NapController`처럼 접미사로 역할을 명확히 — `~Controller`, `~Service`, `~Repository`, `~Provider`)
- 메서드/변수: `camelCase`, 축약어 지양 (`usr` X → `user` O)
- 상수: `UPPER_SNAKE_CASE`, `static final`로 선언하고 매직 넘버/문자열을 직접 코드에 박지 않는다.
  - 나쁜 예: `if (status.equals("PENDING"))`
  - 좋은 예: `private static final String STATUS_PENDING = "PENDING";`

### 함수(메서드)
- **한 메서드는 한 가지 일만 한다.** 메서드 이름을 읽으면 무슨 일을 하는지 바로 알 수 있어야 하고, 실제로 그 일만 해야 한다.
- **길이는 대략 15줄 내외를 넘지 않도록 한다.** 그 이상 길어지면 의미 단위로 private 메서드로 쪼갠다. (한 화면 안에 메서드 전체가 들어오는 정도가 기준)
- **들여쓰기(중첩)는 2단계를 넘기지 않는다.** if/for가 겹겹이 쌓이면 early return이나 메서드 추출로 평탄화한다.
  ```java
  // 지양
  if (user != null) {
      if (user.isActive()) {
          if (user.hasPermission()) { ... }
      }
  }

  // 선호
  if (user == null || !user.isActive() || !user.hasPermission()) {
      return;
  }
  ...
  ```
- 매개변수는 4개를 넘지 않도록 하고, 넘어가면 DTO/record로 묶는다.

### 책임 분리 (SRP)
- 클래스 하나는 변경 이유가 하나여야 한다. "이 클래스가 왜 수정될 수 있지?"를 스스로 물어봤을 때 이유가 두 개 이상 나오면 분리한다.
- 예: 토큰 발급/검증(`JwtProvider`)과 사용자 조회(`CurrentUserIdProvider`)는 이유가 다르므로 별도 클래스로 둔다 (실제로 이렇게 분리되어 있음).
- 서비스 클래스가 여러 도메인의 리포지토리를 직접 여러 개 주무르기 시작하면, 그 경계가 잘못된 신호일 수 있다 — 도메인 재검토.

### 기타
- `null` 반환 대신 `Optional<T>` 또는 명시적 예외(`~NotFoundException` 등)를 사용한다.
- Lombok은 보일러플레이트(getter, builder 등) 용도로만 쓰고, 로직을 숨기는 데 쓰지 않는다.
- 주석은 쓰지 않는다. 코드 자체(명확한 네이밍)로 의도가 드러나야 한다.
- 새 로직을 추가하면 최소 하나 이상의 단위 테스트를 함께 작성한다.

## 빌드 / 테스트

```
./gradlew bootJar -x test   # JAR 빌드 (테스트 제외)
./gradlew test              # 전체 테스트
docker compose up --build   # 로컬 전체 스택 실행 (MySQL, Redis, backend)
```

## 참고 문서

- `docs/backend-priority.md` — 우선순위(P0~P6), 공통 API 규칙(base URL `/api/v1`, 에러 포맷, HTTP 상태 코드 컨벤션, 날짜/시간 포맷)
- `.env.example` — 로컬 환경변수 템플릿. 실제 `.env`는 git에 올리지 않는다.
