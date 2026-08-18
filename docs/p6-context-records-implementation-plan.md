# P6 수면·카페인 기록 API 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 사용자의 실제 취침·기상 시각과 카페인 섭취 시각·용량·잔 수를 안전하게 저장하고 조회·보정·삭제하며, P6 계산에서 재사용할 요약값을 제공한다.

**Architecture:** `sleep`과 `caffeine`을 독립 도메인으로 두고 기존 `CurrentUserIdProvider`와 `UserRepository`를 통해 소유권·사용자 타임존을 적용한다. API는 offset이 포함된 일시를 받아 기록 당시 사용자의 현지 `LocalDateTime`과 IANA 타임존을 함께 저장한다. 계산기는 저장소의 최근 기록만 읽는 순수 계산 경계로 분리한다.

**Tech Stack:** Java 21, Spring Boot 4.1, Spring Data JPA, MySQL 8.4, Flyway, JUnit 5, Mockito, MockMvc, Testcontainers

**Spec:** `docs/p6-context-data-design.md`

## Global Constraints

- LLM은 기록값이나 계산값을 만들지 않는다. 이번 계획의 모든 합계와 평균은 Java 코드가 계산한다.
- 기존 V1~V8 마이그레이션은 수정하지 않고 V9를 추가한다.
- `docs/superpowers` 경로는 생성하지 않는다.
- 커밋 작성자·커미터는 현재 사용자 Git 설정만 사용하며 공동기여자 trailer를 추가하지 않는다.
- 모든 새 동작은 실패 테스트를 먼저 실행한 뒤 최소 구현을 추가한다.

---

### Task 1: V9 마이그레이션과 기록 도메인

**Files:**
- Create: `src/main/resources/db/migration/V9__create_sleep_and_caffeine_logs.sql`
- Create: `src/main/java/com/midtone/backend/sleep/domain/SleepLog.java`
- Create: `src/main/java/com/midtone/backend/sleep/domain/SleepLogSource.java`
- Create: `src/main/java/com/midtone/backend/sleep/domain/SleepLogRepository.java`
- Create: `src/main/java/com/midtone/backend/caffeine/domain/CaffeineIntake.java`
- Create: `src/main/java/com/midtone/backend/caffeine/domain/CaffeineIntakeRepository.java`
- Test: `src/test/java/com/midtone/backend/contextrecord/ContextRecordPersistenceIntegrationTest.java`

**Interfaces:**
- Produces: `SleepLog(long userId, LocalDateTime sleptAt, LocalDateTime wokeAt, String recordedTimezone, SleepLogSource source)`
- Produces: `void SleepLog.update(LocalDateTime sleptAt, LocalDateTime wokeAt, String timezone, SleepLogSource source)`
- Produces: `CaffeineIntake(long userId, LocalDateTime consumedAt, String recordedTimezone, int amountMg, BigDecimal servings, String beverageType)`
- Produces: `void CaffeineIntake.update(LocalDateTime consumedAt, String timezone, int amountMg, BigDecimal servings, String beverageType)`
- Produces: 기간 조회, 소유권 조회, 수면 겹침 조회용 Spring Data repository 메서드

- [ ] **Step 1: 실제 MySQL 저장 실패 테스트 작성**

```java
@Test
void 수면과_카페인_기록을_저장한다() {
    SleepLog sleep = sleepLogRepository.save(new SleepLog(
            user.getId(), LocalDateTime.parse("2026-08-17T23:30:00"),
            LocalDateTime.parse("2026-08-18T07:10:00"), "Asia/Seoul", SleepLogSource.MANUAL));
    CaffeineIntake intake = caffeineIntakeRepository.save(new CaffeineIntake(
            user.getId(), LocalDateTime.parse("2026-08-18T09:00:00"),
            "Asia/Seoul", 120, new BigDecimal("1.00"), "COFFEE"));

    assertNotNull(sleep.getId());
    assertNotNull(intake.getId());
}
```

- [ ] **Step 2: RED 확인** — `./gradlew test --tests "*ContextRecordPersistenceIntegrationTest"`; 새 타입이 없어 컴파일 실패해야 한다.
- [ ] **Step 3: V9와 엔티티·repository 최소 구현** — FK는 `users(id) ON DELETE CASCADE`, 수면 인덱스 `(user_id, woke_at)`, 카페인 인덱스 `(user_id, consumed_at)`를 둔다.
- [ ] **Step 4: GREEN 확인** — 동일 테스트가 통과해야 한다.
- [ ] **Step 5: 커밋** — `feat : 수면 및 카페인 기록 도메인 추가`

### Task 2: 수면 기록 서비스

**Files:**
- Create: `src/main/java/com/midtone/backend/sleep/application/SleepLogService.java`
- Create: `src/main/java/com/midtone/backend/sleep/application/SleepLogException.java`
- Create: `src/main/java/com/midtone/backend/sleep/application/CreateSleepLogRequest.java`
- Create: `src/main/java/com/midtone/backend/sleep/application/UpdateSleepLogRequest.java`
- Create: `src/main/java/com/midtone/backend/sleep/application/SleepLogResponse.java`
- Create: `src/main/java/com/midtone/backend/sleep/application/SleepLogListResponse.java`
- Create: `src/main/java/com/midtone/backend/global/time/ClockConfig.java`
- Test: `src/test/java/com/midtone/backend/sleep/application/SleepLogServiceTest.java`

**Interfaces:**
- Produces: `SleepLogResponse create(CreateSleepLogRequest request)`
- Produces: `SleepLogListResponse getLogs(LocalDate from, LocalDate to)`
- Produces: `SleepLogResponse update(long sleepLogId, UpdateSleepLogRequest request)`
- Produces: `void delete(long sleepLogId)`
- Request timestamps: `OffsetDateTime sleptAt`, `OffsetDateTime wokeAt`; response timestamps도 `OffsetDateTime`

- [ ] **Step 1: 생성·검증·소유권 실패 테스트 작성**

```java
@Test
void 기상시각이_취침시각보다_늦지_않으면_거절한다() {
    CreateSleepLogRequest request = new CreateSleepLogRequest(
            OffsetDateTime.parse("2026-08-18T08:00:00+09:00"),
            OffsetDateTime.parse("2026-08-18T07:00:00+09:00"), "MANUAL");

    SleepLogException error = assertThrows(SleepLogException.class, () -> service.create(request));
    assertEquals(SleepLogException.ErrorCode.INVALID_INTERVAL, error.getErrorCode());
}

@Test
void 기존_수면과_겹치면_거절한다() {
    given(repository.existsOverlapping(eq(1L), any(), any())).willReturn(true);
    assertThrows(SleepLogException.class, () -> service.create(validRequest));
}
```

- [ ] **Step 2: RED 확인** — `./gradlew test --tests "*SleepLogServiceTest"`; 서비스 타입 부재로 실패해야 한다.
- [ ] **Step 3: 최소 서비스 구현** — `ClockConfig`의 `Clock.system(DateTimeDefaults.DEFAULT_ZONE)`을 주입하고, 사용자 타임존 변환, 미래·순서·겹침 검증, 기간 조회, 수정·삭제 소유권을 구현한다. 단위 테스트는 `Clock.fixed(...)`를 직접 전달한다.
- [ ] **Step 4: GREEN 확인** — 서비스 테스트 전체가 통과해야 한다.
- [ ] **Step 5: 커밋** — `feat : 수면 기록 서비스 추가`

### Task 3: 카페인 기록 서비스

**Files:**
- Create: `src/main/java/com/midtone/backend/caffeine/application/CaffeineIntakeService.java`
- Create: `src/main/java/com/midtone/backend/caffeine/application/CaffeineIntakeException.java`
- Create: `src/main/java/com/midtone/backend/caffeine/application/CreateCaffeineIntakeRequest.java`
- Create: `src/main/java/com/midtone/backend/caffeine/application/UpdateCaffeineIntakeRequest.java`
- Create: `src/main/java/com/midtone/backend/caffeine/application/CaffeineIntakeResponse.java`
- Create: `src/main/java/com/midtone/backend/caffeine/application/CaffeineIntakeListResponse.java`
- Test: `src/test/java/com/midtone/backend/caffeine/application/CaffeineIntakeServiceTest.java`

**Interfaces:**
- Produces: `CaffeineIntakeResponse create(CreateCaffeineIntakeRequest request)`
- Produces: `CaffeineIntakeListResponse getIntakes(LocalDate from, LocalDate to)` with `totalAmountMg`, `totalServings`
- Produces: `CaffeineIntakeResponse update(long intakeId, UpdateCaffeineIntakeRequest request)`
- Produces: `void delete(long intakeId)`

- [ ] **Step 1: 입력·합계·소유권 실패 테스트 작성**

```java
@Test
void 기간_조회는_mg와_잔_수를_각각_합산한다() {
    given(repository.findByUserIdAndConsumedAtBetweenOrderByConsumedAtAsc(eq(1L), any(), any()))
            .willReturn(List.of(intake(80, "1.00"), intake(120, "0.50")));

    CaffeineIntakeListResponse result = service.getIntakes(
            LocalDate.parse("2026-08-18"), LocalDate.parse("2026-08-18"));

    assertEquals(200, result.totalAmountMg());
    assertEquals(new BigDecimal("1.50"), result.totalServings());
}
```

- [ ] **Step 2: RED 확인** — `./gradlew test --tests "*CaffeineIntakeServiceTest"`; 서비스 타입 부재로 실패해야 한다.
- [ ] **Step 3: 최소 서비스 구현** — 양수·미래 검증, 기본 servings 1, 기간 합계, 수정·삭제 소유권을 구현한다.
- [ ] **Step 4: GREEN 확인** — 서비스 테스트 전체가 통과해야 한다.
- [ ] **Step 5: 커밋** — `feat : 카페인 섭취 기록 서비스 추가`

### Task 4: 기록 HTTP API와 예외 매핑

**Files:**
- Create: `src/main/java/com/midtone/backend/sleep/SleepLogController.java`
- Create: `src/main/java/com/midtone/backend/caffeine/CaffeineIntakeController.java`
- Modify: `src/main/java/com/midtone/backend/global/error/GlobalExceptionHandler.java`
- Test: `src/test/java/com/midtone/backend/sleep/SleepLogControllerTest.java`
- Test: `src/test/java/com/midtone/backend/caffeine/CaffeineIntakeControllerTest.java`

**Interfaces:**
- Routes: `POST/GET /api/v1/sleep-logs`, `PATCH/DELETE /api/v1/sleep-logs/{id}`
- Routes: `POST/GET /api/v1/caffeine-intakes`, `PATCH/DELETE /api/v1/caffeine-intakes/{id}`

- [ ] **Step 1: HTTP 계약 실패 테스트 작성**

```java
mockMvc.perform(post("/api/v1/sleep-logs")
        .contentType("application/json")
        .content("{\"sleptAt\":\"2026-08-17T23:30:00+09:00\",\"wokeAt\":\"2026-08-18T07:10:00+09:00\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sleepLogId").value(1));
```

- [ ] **Step 2: RED 확인** — 두 controller test를 실행해 404 또는 타입 부재 실패를 확인한다.
- [ ] **Step 3: 컨트롤러와 예외 핸들러 최소 구현** — `@Valid`, ISO 날짜 쿼리, 201/200/204를 정확히 매핑한다.
- [ ] **Step 4: GREEN 확인** — 두 controller test가 통과해야 한다.
- [ ] **Step 5: 커밋** — `feat : 수면 및 카페인 기록 API 추가`

### Task 5: 기록 기반 계산기

**Files:**
- Create: `src/main/java/com/midtone/backend/sleep/application/SleepPatternCalculator.java`
- Create: `src/main/java/com/midtone/backend/caffeine/application/CaffeineStatusCalculator.java`
- Test: `src/test/java/com/midtone/backend/sleep/application/SleepPatternCalculatorTest.java`
- Test: `src/test/java/com/midtone/backend/caffeine/application/CaffeineStatusCalculatorTest.java`

**Interfaces:**
- Produces: `SleepPattern calculate(long userId, LocalDate today)` with habitual bedtime, wake time, midpoint, CBTmin, sample count
- Produces: `DailyCaffeineStatus calculate(long userId, LocalDate date)` with total mg, total servings, over-limit boolean

- [ ] **Step 1: 자정 경계와 잔 수 경고 실패 테스트 작성**

```java
@Test
void 자정_전후_취침시각을_밤_시각으로_평균한다() {
    given(repository.findRecent(...)).willReturn(List.of(
            sleep("2026-08-16T23:00:00", "2026-08-17T07:00:00"),
            sleep("2026-08-18T01:00:00", "2026-08-18T09:00:00")));
    assertEquals(LocalTime.MIDNIGHT, calculator.calculate(1L, LocalDate.parse("2026-08-18")).habitualBedtime());
}

@Test
void 하루_잔_수가_두_잔을_초과하면_경고한다() {
    assertTrue(calculator.calculate(1L, LocalDate.parse("2026-08-18")).overServingLimit());
}
```

- [ ] **Step 2: RED 확인** — 두 calculator test가 타입 부재로 실패해야 한다.
- [ ] **Step 3: 최소 계산 구현** — 최근 14일, 최소 개수 없음, 원형 평균, CBTmin 2시간 전, `servings > 2` 판정을 구현한다.
- [ ] **Step 4: GREEN 확인** — 계산기 테스트가 통과해야 한다.
- [ ] **Step 5: 전체 회귀 검증** — `./gradlew test --no-daemon`과 Docker 기반 애플리케이션 기동을 확인한다.
- [ ] **Step 6: 커밋** — `feat : 수면 패턴 및 카페인 상태 계산 추가`

## Self-Review

- 스펙 커버리지: 두 기록 테이블(Task 1), CRUD·검증·소유권(Task 2~4), 기간 합계·최근 수면 패턴·CBTmin·잔 수 경고(Task 3·5)를 모두 포함한다.
- 제외 범위 준수: OpenAI, Chat, Nutrition, 전환일 오프셋은 별도 계획으로 남기며 이 계획에 섞지 않는다.
- 타입 일관성: 외부 일시는 `OffsetDateTime`, DB 현지 시각은 `LocalDateTime`, 날짜 필터는 `LocalDate`, 잔 수는 `BigDecimal`로 통일한다.
- placeholder 점검: TBD/TODO/후속 구현 지시 없이 각 태스크의 입력·출력·실패 조건·검증 명령을 명시했다.
