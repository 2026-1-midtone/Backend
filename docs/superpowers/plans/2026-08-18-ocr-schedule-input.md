# P5 OCR 일정 입력 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 근무표 이미지를 업로드하면 Document AI Form Parser로 일정 초안을 만들고, 검수·보정·확정을 거쳐 실제 근무 일정에 반영한다.

**Architecture:** `ocr` 패키지에 잡(OcrJob)·초안(OcrDraftShift) 도메인을 추가한다. 업로드 시 이미지를 MySQL BLOB으로 저장하고 `@Async` 워커가 Document AI를 호출해 초안을 생성한다. 확정 시 기존 일정을 덮어쓰고 코칭을 재생성한다.

**Tech Stack:** Java 21, Spring Boot 4.1, Flyway(V7), RestClient + google-auth-library(ADC impersonation), MockRestServiceServer, Testcontainers.

**Spec:** `docs/superpowers/specs/2026-08-18-ocr-schedule-input-design.md`

## Global Constraints

- Base URL `/api/v1`, 오류 응답 `{ "message": "..." }`, 상태 코드 400/401/403/404/409 규칙 준수.
- 소유권 검사는 서비스 계층에서 수행한다.
- 예외는 도메인별 `OcrException` + 내부 `ErrorCode` enum(메시지·HttpStatus) 패턴을 따르고 `GlobalExceptionHandler`에 등록한다.
- 컨트롤러 테스트는 `@WebMvcTest + @AutoConfigureMockMvc(addFilters = false) + @MockitoBean`, 통합 테스트는 `support.IntegrationTest` 상속 + `TestUserFixture` 패턴을 따른다.
- 테스트 실행: `./gradlew test --tests "<클래스명>"` (Windows는 `gradlew.bat`). 통합 테스트는 Docker 필요.
- 커밋 메시지는 기존 스타일(`feat : ...`, `test : ...`)을 따르고 `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`를 붙인다.

---

### Task 1: V7 마이그레이션 + 도메인 엔티티

**Files:**
- Create: `src/main/resources/db/migration/V7__create_ocr_tables.sql`
- Create: `src/main/java/com/midtone/backend/ocr/domain/OcrJobStatus.java`
- Create: `src/main/java/com/midtone/backend/ocr/domain/OcrJob.java`
- Create: `src/main/java/com/midtone/backend/ocr/domain/OcrJobRepository.java`
- Create: `src/main/java/com/midtone/backend/ocr/domain/OcrDraftShift.java`
- Create: `src/main/java/com/midtone/backend/ocr/domain/OcrDraftShiftRepository.java`
- Test: `src/test/java/com/midtone/backend/ocr/domain/OcrJobTest.java`

**Interfaces:**
- Produces: `OcrJob(Long userId, byte[] image, String imageMime, String targetMonth)` 생성자(초기 상태 PENDING), `markProcessing()`, `markCompleted()`, `markFailed(String)`, `markConfirmed()`(이미지 NULL 처리), `isOwnedBy(long)`, getters. `OcrDraftShift(Long jobId, LocalDate workDate, ShiftType shiftType, BigDecimal confidence)`, `applyCorrection(LocalDate, ShiftType, LocalTime, LocalTime, Boolean excluded)`, getters. `OcrJobRepository.findByStatus(OcrJobStatus)`, `OcrDraftShiftRepository.findByJobIdOrderByWorkDateAsc(Long)`, `deleteByJobId(Long)`.

- [ ] **Step 1: 실패하는 테스트 작성** — `OcrJobTest.java`

```java
package com.midtone.backend.ocr.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OcrJobTest {

    @Test
    void 생성_직후에는_PENDING_상태다() {
        OcrJob job = new OcrJob(1L, new byte[] {1, 2}, "image/png", "2026-08");
        assertEquals(OcrJobStatus.PENDING, job.getStatus());
    }

    @Test
    void 상태_전이가_순서대로_동작한다() {
        OcrJob job = new OcrJob(1L, new byte[] {1}, "image/png", "2026-08");
        job.markProcessing();
        assertEquals(OcrJobStatus.PROCESSING, job.getStatus());
        job.markCompleted();
        assertEquals(OcrJobStatus.COMPLETED, job.getStatus());
    }

    @Test
    void 실패_시_사유를_기록한다() {
        OcrJob job = new OcrJob(1L, new byte[] {1}, "image/png", "2026-08");
        job.markProcessing();
        job.markFailed("근무표를 인식하지 못했습니다.");
        assertEquals(OcrJobStatus.FAILED, job.getStatus());
        assertEquals("근무표를 인식하지 못했습니다.", job.getErrorMessage());
    }

    @Test
    void 확정하면_이미지를_비운다() {
        OcrJob job = new OcrJob(1L, new byte[] {1}, "image/png", "2026-08");
        job.markProcessing();
        job.markCompleted();
        job.markConfirmed();
        assertEquals(OcrJobStatus.CONFIRMED, job.getStatus());
        assertNull(job.getImage());
    }

    @Test
    void 소유자를_판별한다() {
        OcrJob job = new OcrJob(7L, new byte[] {1}, "image/png", "2026-08");
        assertTrue(job.isOwnedBy(7L));
        assertFalse(job.isOwnedBy(8L));
    }
}
```

- [ ] **Step 2: 테스트 실패 확인** — `./gradlew test --tests "com.midtone.backend.ocr.domain.OcrJobTest"` → 컴파일 에러(클래스 없음) 확인
- [ ] **Step 3: V7 마이그레이션 작성**

```sql
CREATE TABLE ocr_jobs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    image MEDIUMBLOB,
    image_mime VARCHAR(30),
    target_month VARCHAR(7) NOT NULL,
    error_message VARCHAR(255),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_ocr_jobs_user (user_id),
    CONSTRAINT fk_ocr_jobs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ocr_draft_shifts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    shift_type VARCHAR(20) NOT NULL,
    start_time TIME,
    end_time TIME,
    confidence DECIMAL(4,3),
    excluded BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ocr_draft_shifts_job_date (job_id, work_date),
    CONSTRAINT fk_ocr_draft_shifts_job FOREIGN KEY (job_id) REFERENCES ocr_jobs (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- [ ] **Step 4: 엔티티·리포지토리 구현** — `OcrJobStatus`:

```java
package com.midtone.backend.ocr.domain;

public enum OcrJobStatus {
    PENDING, PROCESSING, COMPLETED, FAILED, CONFIRMED
}
```

`OcrJob` (기존 `ShiftSchedule` 스타일: protected 기본 생성자, insertable/updatable=false 타임스탬프):

```java
package com.midtone.backend.ocr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "ocr_jobs")
public class OcrJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OcrJobStatus status;

    @Lob
    @Column(columnDefinition = "MEDIUMBLOB")
    private byte[] image;

    @Column(name = "image_mime", length = 30)
    private String imageMime;

    @Column(name = "target_month", nullable = false, length = 7)
    private String targetMonth;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected OcrJob() {
    }

    public OcrJob(Long userId, byte[] image, String imageMime, String targetMonth) {
        this.userId = userId;
        this.image = image;
        this.imageMime = imageMime;
        this.targetMonth = targetMonth;
        this.status = OcrJobStatus.PENDING;
    }

    public void markProcessing() {
        this.status = OcrJobStatus.PROCESSING;
        this.errorMessage = null;
    }

    public void markCompleted() {
        this.status = OcrJobStatus.COMPLETED;
    }

    public void markFailed(String errorMessage) {
        this.status = OcrJobStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void markConfirmed() {
        this.status = OcrJobStatus.CONFIRMED;
        this.image = null;
    }

    public boolean isOwnedBy(long userId) {
        return this.userId != null && this.userId == userId;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public OcrJobStatus getStatus() { return status; }
    public byte[] getImage() { return image; }
    public String getImageMime() { return imageMime; }
    public String getTargetMonth() { return targetMonth; }
    public String getErrorMessage() { return errorMessage; }
}
```

`OcrDraftShift`:

```java
package com.midtone.backend.ocr.domain;

import com.midtone.backend.shift.domain.ShiftType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "ocr_draft_shifts")
public class OcrDraftShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false, length = 20)
    private ShiftType shiftType;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column
    private BigDecimal confidence;

    @Column(nullable = false)
    private boolean excluded;

    protected OcrDraftShift() {
    }

    public OcrDraftShift(Long jobId, LocalDate workDate, ShiftType shiftType, BigDecimal confidence) {
        this.jobId = jobId;
        this.workDate = workDate;
        this.shiftType = shiftType;
        this.confidence = confidence;
        this.excluded = false;
    }

    public void applyCorrection(
            LocalDate workDate, ShiftType shiftType, LocalTime startTime, LocalTime endTime, Boolean excluded) {
        if (workDate != null) {
            this.workDate = workDate;
        }
        if (shiftType != null) {
            this.shiftType = shiftType;
        }
        if (startTime != null) {
            this.startTime = startTime;
        }
        if (endTime != null) {
            this.endTime = endTime;
        }
        if (excluded != null) {
            this.excluded = excluded;
        }
    }

    public Long getId() { return id; }
    public Long getJobId() { return jobId; }
    public LocalDate getWorkDate() { return workDate; }
    public ShiftType getShiftType() { return shiftType; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public BigDecimal getConfidence() { return confidence; }
    public boolean isExcluded() { return excluded; }
}
```

리포지토리:

```java
package com.midtone.backend.ocr.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OcrJobRepository extends JpaRepository<OcrJob, Long> {

    List<OcrJob> findByStatus(OcrJobStatus status);
}
```

```java
package com.midtone.backend.ocr.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OcrDraftShiftRepository extends JpaRepository<OcrDraftShift, Long> {

    List<OcrDraftShift> findByJobIdOrderByWorkDateAsc(Long jobId);

    void deleteByJobId(Long jobId);
}
```

- [ ] **Step 5: 테스트 통과 확인** — `./gradlew test --tests "com.midtone.backend.ocr.domain.OcrJobTest"` → PASS
- [ ] **Step 6: 커밋** — `git add`(위 파일들) 후 `feat : OCR 잡·초안 도메인 및 V7 마이그레이션 추가`

---

### Task 2: OcrException + GlobalExceptionHandler 등록

**Files:**
- Create: `src/main/java/com/midtone/backend/ocr/application/OcrException.java`
- Modify: `src/main/java/com/midtone/backend/global/error/GlobalExceptionHandler.java` (`handleRoutineException` 아래에 핸들러 추가)

**Interfaces:**
- Produces: `OcrException(ErrorCode)` — ErrorCode: `IMAGE_REQUIRED`, `UNSUPPORTED_IMAGE_TYPE`, `IMAGE_TOO_LARGE`, `INVALID_MONTH`(400) / `JOB_NOT_FOUND`, `DRAFT_NOT_FOUND`(404) / `JOB_ACCESS_DENIED`(403) / `JOB_NOT_COMPLETED`, `JOB_NOT_RETRYABLE`(409). `getErrorCode().getStatus()`, `getMessage()`.

- [ ] **Step 1: OcrException 작성** (`ShiftException`과 동일 구조)

```java
package com.midtone.backend.ocr.application;

import org.springframework.http.HttpStatus;

public class OcrException extends RuntimeException {

    private final ErrorCode errorCode;

    public OcrException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        IMAGE_REQUIRED("근무표 이미지는 필수입니다.", HttpStatus.BAD_REQUEST),
        UNSUPPORTED_IMAGE_TYPE("JPEG 또는 PNG 이미지만 업로드할 수 있습니다.", HttpStatus.BAD_REQUEST),
        IMAGE_TOO_LARGE("이미지는 최대 10MB까지 업로드할 수 있습니다.", HttpStatus.BAD_REQUEST),
        INVALID_MONTH("month는 yyyy-MM 형식이어야 합니다.", HttpStatus.BAD_REQUEST),
        JOB_NOT_FOUND("해당 OCR 작업이 없습니다.", HttpStatus.NOT_FOUND),
        DRAFT_NOT_FOUND("해당 초안 항목이 없습니다.", HttpStatus.NOT_FOUND),
        JOB_ACCESS_DENIED("접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
        JOB_NOT_COMPLETED("분석이 완료된 작업만 검수·확정할 수 있습니다.", HttpStatus.CONFLICT),
        JOB_NOT_RETRYABLE("실패했거나 완료된 작업만 재시도할 수 있습니다.", HttpStatus.CONFLICT);

        private final String message;
        private final HttpStatus status;

        ErrorCode(String message, HttpStatus status) {
            this.message = message;
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }
}
```

- [ ] **Step 2: GlobalExceptionHandler에 핸들러 추가** — import `com.midtone.backend.ocr.application.OcrException;` 추가 후 `handleRoutineException` 메서드 아래에:

```java
@ExceptionHandler(OcrException.class)
public ResponseEntity<ErrorResponse> handleOcrException(OcrException exception) {
    return ResponseEntity.status(exception.getErrorCode().getStatus())
            .body(new ErrorResponse(exception.getMessage()));
}
```

- [ ] **Step 3: 컴파일 확인** — `./gradlew compileJava` → BUILD SUCCESSFUL
- [ ] **Step 4: 커밋** — `feat : OCR 예외 및 전역 핸들러 등록`

---

### Task 3: OcrDraftParser (Form Parser 응답 → 초안)

**Files:**
- Create: `src/main/java/com/midtone/backend/ocr/application/OcrDraftParser.java`
- Create: `src/test/resources/ocr/form-parser-response.json` (픽스처)
- Test: `src/test/java/com/midtone/backend/ocr/application/OcrDraftParserTest.java`

**Interfaces:**
- Consumes: Jackson `JsonNode` (Document AI `document` 노드: `text`, `pages[].tables[].bodyRows[].cells[].layout`)
- Produces: `OcrDraftParser.parse(JsonNode document, YearMonth targetMonth)` → `List<OcrDraftParser.ParsedDraft>`; `record ParsedDraft(LocalDate workDate, ShiftType shiftType, BigDecimal confidence)`. 날짜 오름차순, 날짜 중복 시 첫 항목 유지.

- [ ] **Step 1: 픽스처 작성** — `src/test/resources/ocr/form-parser-response.json`. Document AI 실제 구조를 축약(문자 인덱스가 `text`와 정확히 일치해야 함). `text`는 `"날짜 근무 1 D 2 N 3 OFF 2026-08-04 이브닝 ? X"` (공백 구분, 총 43자):

```json
{
  "text": "날짜 근무 1 D 2 N 3 OFF 2026-08-04 이브닝 ? X",
  "pages": [
    {
      "tables": [
        {
          "headerRows": [
            {
              "cells": [
                {"layout": {"textAnchor": {"textSegments": [{"startIndex": "0", "endIndex": "2"}]}, "confidence": 0.99}},
                {"layout": {"textAnchor": {"textSegments": [{"startIndex": "3", "endIndex": "5"}]}, "confidence": 0.99}}
              ]
            }
          ],
          "bodyRows": [
            {
              "cells": [
                {"layout": {"textAnchor": {"textSegments": [{"startIndex": "6", "endIndex": "7"}]}, "confidence": 0.98}},
                {"layout": {"textAnchor": {"textSegments": [{"startIndex": "8", "endIndex": "9"}]}, "confidence": 0.97}}
              ]
            },
            {
              "cells": [
                {"layout": {"textAnchor": {"textSegments": [{"startIndex": "10", "endIndex": "11"}]}, "confidence": 0.96}},
                {"layout": {"textAnchor": {"textSegments": [{"startIndex": "12", "endIndex": "13"}]}, "confidence": 0.95}}
              ]
            },
            {
              "cells": [
                {"layout": {"textAnchor": {"textSegments": [{"startIndex": "14", "endIndex": "15"}]}, "confidence": 0.94}},
                {"layout": {"textAnchor": {"textSegments": [{"startIndex": "16", "endIndex": "19"}]}, "confidence": 0.93}}
              ]
            },
            {
              "cells": [
                {"layout": {"textAnchor": {"textSegments": [{"startIndex": "20", "endIndex": "30"}]}, "confidence": 0.92}},
                {"layout": {"textAnchor": {"textSegments": [{"startIndex": "31", "endIndex": "34"}]}, "confidence": 0.91}}
              ]
            },
            {
              "cells": [
                {"layout": {"textAnchor": {"textSegments": [{"startIndex": "35", "endIndex": "36"}]}, "confidence": 0.5}},
                {"layout": {"textAnchor": {"textSegments": [{"startIndex": "37", "endIndex": "38"}]}, "confidence": 0.5}}
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

주의: 위 인덱스는 계획 작성 시점 추정이므로, 구현 시 `text` 문자열의 실제 인덱스(자바 `String.indexOf`)로 검산해 맞출 것. 마지막 행(`?` / `X`)은 날짜 파싱 실패 → 제외 검증용.

- [ ] **Step 2: 실패하는 테스트 작성** — `OcrDraftParserTest.java`

```java
package com.midtone.backend.ocr.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.midtone.backend.shift.domain.ShiftType;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OcrDraftParserTest {

    private final OcrDraftParser parser = new OcrDraftParser();
    private JsonNode document;

    @BeforeEach
    void setUp() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/ocr/form-parser-response.json")) {
            document = new ObjectMapper().readTree(in);
        }
    }

    @Test
    void 표의_각_행을_날짜와_근무유형_초안으로_변환한다() {
        List<OcrDraftParser.ParsedDraft> drafts = parser.parse(document, YearMonth.of(2026, 8));

        assertEquals(4, drafts.size());
        assertEquals(LocalDate.of(2026, 8, 1), drafts.get(0).workDate());
        assertEquals(ShiftType.DAY, drafts.get(0).shiftType());
        assertEquals(LocalDate.of(2026, 8, 2), drafts.get(1).workDate());
        assertEquals(ShiftType.NIGHT, drafts.get(1).shiftType());
        assertEquals(LocalDate.of(2026, 8, 3), drafts.get(2).workDate());
        assertEquals(ShiftType.OFF, drafts.get(2).shiftType());
        assertEquals(LocalDate.of(2026, 8, 4), drafts.get(3).workDate());
        assertEquals(ShiftType.EVENING, drafts.get(3).shiftType());
    }

    @Test
    void 근무_셀의_confidence를_초안에_기록한다() {
        List<OcrDraftParser.ParsedDraft> drafts = parser.parse(document, YearMonth.of(2026, 8));
        assertEquals(new BigDecimal("0.970"), drafts.get(0).confidence());
    }

    @Test
    void 날짜나_근무유형을_해석하지_못한_행은_제외한다() {
        // 픽스처 마지막 행("?", "X"에서 "?"는 날짜 아님) → 초안 4건만 생성
        List<OcrDraftParser.ParsedDraft> drafts = parser.parse(document, YearMonth.of(2026, 8));
        assertEquals(4, drafts.size());
    }

    @Test
    void 표가_없으면_빈_목록을_반환한다() throws Exception {
        JsonNode empty = new ObjectMapper().readTree("{\"text\":\"\",\"pages\":[]}");
        assertEquals(0, parser.parse(empty, YearMonth.of(2026, 8)).size());
    }
}
```

- [ ] **Step 3: 테스트 실패 확인** — `./gradlew test --tests "com.midtone.backend.ocr.application.OcrDraftParserTest"` → 컴파일 에러 확인
- [ ] **Step 4: 파서 구현**

```java
package com.midtone.backend.ocr.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.midtone.backend.shift.domain.ShiftType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class OcrDraftParser {

    public record ParsedDraft(LocalDate workDate, ShiftType shiftType, BigDecimal confidence) {
    }

    private static final Pattern DAY_ONLY = Pattern.compile("^(\\d{1,2})일?$");
    private static final Pattern MONTH_DAY = Pattern.compile("^(\\d{1,2})[/월.\\s]\\s*(\\d{1,2})일?$");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final Map<String, ShiftType> SHIFT_CODES = Map.ofEntries(
            Map.entry("D", ShiftType.DAY), Map.entry("DAY", ShiftType.DAY),
            Map.entry("데이", ShiftType.DAY), Map.entry("주간", ShiftType.DAY),
            Map.entry("E", ShiftType.EVENING), Map.entry("EVE", ShiftType.EVENING),
            Map.entry("EVENING", ShiftType.EVENING), Map.entry("이브닝", ShiftType.EVENING),
            Map.entry("이브", ShiftType.EVENING),
            Map.entry("N", ShiftType.NIGHT), Map.entry("NIGHT", ShiftType.NIGHT),
            Map.entry("나이트", ShiftType.NIGHT), Map.entry("야간", ShiftType.NIGHT),
            Map.entry("OFF", ShiftType.OFF), Map.entry("O", ShiftType.OFF),
            Map.entry("X", ShiftType.OFF), Map.entry("휴", ShiftType.OFF),
            Map.entry("휴무", ShiftType.OFF), Map.entry("오프", ShiftType.OFF));

    public List<ParsedDraft> parse(JsonNode document, YearMonth targetMonth) {
        String fullText = document.path("text").asText("");
        Map<LocalDate, ParsedDraft> byDate = new LinkedHashMap<>();
        for (JsonNode page : document.path("pages")) {
            for (JsonNode table : page.path("tables")) {
                for (JsonNode row : table.path("bodyRows")) {
                    parseRow(row, fullText, targetMonth)
                            .ifPresent(draft -> byDate.putIfAbsent(draft.workDate(), draft));
                }
            }
        }
        List<ParsedDraft> drafts = new ArrayList<>(byDate.values());
        drafts.sort((a, b) -> a.workDate().compareTo(b.workDate()));
        return drafts;
    }

    private java.util.Optional<ParsedDraft> parseRow(JsonNode row, String fullText, YearMonth targetMonth) {
        LocalDate workDate = null;
        int dateCellIndex = -1;
        JsonNode cells = row.path("cells");
        for (int i = 0; i < cells.size(); i++) {
            workDate = parseDate(cellText(cells.get(i), fullText), targetMonth);
            if (workDate != null) {
                dateCellIndex = i;
                break;
            }
        }
        if (workDate == null) {
            return java.util.Optional.empty();
        }
        for (int i = dateCellIndex + 1; i < cells.size(); i++) {
            JsonNode cell = cells.get(i);
            ShiftType shiftType = SHIFT_CODES.get(cellText(cell, fullText).toUpperCase());
            if (shiftType != null) {
                return java.util.Optional.of(new ParsedDraft(workDate, shiftType, cellConfidence(cell)));
            }
        }
        return java.util.Optional.empty();
    }

    private String cellText(JsonNode cell, String fullText) {
        StringBuilder text = new StringBuilder();
        for (JsonNode segment : cell.path("layout").path("textAnchor").path("textSegments")) {
            int start = segment.path("startIndex").asInt(0);
            int end = segment.path("endIndex").asInt(0);
            if (start >= 0 && end <= fullText.length() && start < end) {
                text.append(fullText, start, end);
            }
        }
        return text.toString().trim();
    }

    private BigDecimal cellConfidence(JsonNode cell) {
        double confidence = cell.path("layout").path("confidence").asDouble(0.0);
        return BigDecimal.valueOf(confidence).setScale(3, RoundingMode.HALF_UP);
    }

    private LocalDate parseDate(String text, YearMonth targetMonth) {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text, ISO_DATE);
        } catch (DateTimeParseException ignored) {
        }
        Matcher monthDay = MONTH_DAY.matcher(text);
        if (monthDay.matches()) {
            return buildDate(targetMonth.getYear(), Integer.parseInt(monthDay.group(1)), Integer.parseInt(monthDay.group(2)));
        }
        Matcher dayOnly = DAY_ONLY.matcher(text);
        if (dayOnly.matches()) {
            return buildDate(targetMonth.getYear(), targetMonth.getMonthValue(), Integer.parseInt(dayOnly.group(1)));
        }
        return null;
    }

    private LocalDate buildDate(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        } catch (java.time.DateTimeException e) {
            return null;
        }
    }
}
```

- [ ] **Step 5: 테스트 통과 확인** — 같은 명령 → PASS (픽스처 인덱스 불일치 시 Step 1의 주의사항대로 인덱스 수정)
- [ ] **Step 6: 커밋** — `feat : Form Parser 응답을 일정 초안으로 변환하는 파서 추가`

---

### Task 4: DocumentAiClient

**Files:**
- Modify: `build.gradle` — dependencies에 `implementation 'com.google.auth:google-auth-library-oauth2-http:1.30.0'` 추가 (해당 버전이 해석 안 되면 mavenCentral의 최신 1.x로 조정)
- Create: `src/main/java/com/midtone/backend/ocr/documentai/DocumentAiClient.java`
- Create: `src/main/java/com/midtone/backend/ocr/documentai/DocumentAiAccessTokenProvider.java`
- Modify: `src/main/resources/application.yml`
- Test: `src/test/java/com/midtone/backend/ocr/documentai/DocumentAiClientTest.java`

**Interfaces:**
- Produces: `DocumentAiClient.process(byte[] image, String mimeType)` → `JsonNode`(응답의 `document` 노드). 실패 시 `DocumentAiClient.DocumentAiCallException extends RuntimeException`. `DocumentAiAccessTokenProvider`는 `String getAccessToken()` 단일 메서드 인터페이스이며 기본 구현 `AdcAccessTokenProvider`(`@Component`)가 ADC에서 토큰 발급.

- [ ] **Step 1: application.yml에 설정 추가** — `app:` 블록에:

```yaml
  documentai:
    endpoint: ${DOCUMENTAI_ENDPOINT:https://us-documentai.googleapis.com}
    project-number: ${DOCUMENTAI_PROJECT_NUMBER:437332095325}
    location: ${DOCUMENTAI_LOCATION:us}
    processor-id: ${DOCUMENTAI_PROCESSOR_ID:cbb47f2525db9dc0}
    quota-project: ${DOCUMENTAI_QUOTA_PROJECT:shiftmate-504210}
```

그리고 `spring:` 블록에 multipart 한도:

```yaml
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 12MB
```

- [ ] **Step 2: 실패하는 테스트 작성** — `DocumentAiClientTest.java`

```java
package com.midtone.backend.ocr.documentai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DocumentAiClientTest {

    private static final String EXPECTED_URL =
            "https://us-documentai.googleapis.com/v1/projects/437332095325/locations/us/processors/cbb47f2525db9dc0:process";

    private MockRestServiceServer server;
    private DocumentAiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DocumentAiClient(
                builder, () -> "test-token",
                "https://us-documentai.googleapis.com", "437332095325", "us", "cbb47f2525db9dc0", "shiftmate-504210");
    }

    @Test
    void 프로세서에_이미지를_전송하고_document_노드를_반환한다() {
        byte[] image = new byte[] {1, 2, 3};
        server.expect(requestTo(EXPECTED_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(header("x-goog-user-project", "shiftmate-504210"))
                .andExpect(jsonPath("$.rawDocument.content").value(Base64.getEncoder().encodeToString(image)))
                .andExpect(jsonPath("$.rawDocument.mimeType").value("image/png"))
                .andRespond(withSuccess("{\"document\":{\"text\":\"hello\"}}", MediaType.APPLICATION_JSON));

        JsonNode document = client.process(image, "image/png");

        assertEquals("hello", document.path("text").asText());
        server.verify();
    }

    @Test
    void 서버_오류면_DocumentAiCallException을_던진다() {
        server.expect(requestTo(EXPECTED_URL)).andRespond(withServerError());
        assertThrows(DocumentAiClient.DocumentAiCallException.class,
                () -> client.process(new byte[] {1}, "image/png"));
    }
}
```

- [ ] **Step 3: 테스트 실패 확인** — `./gradlew test --tests "com.midtone.backend.ocr.documentai.DocumentAiClientTest"` → 컴파일 에러
- [ ] **Step 4: 구현** — `DocumentAiAccessTokenProvider.java`:

```java
package com.midtone.backend.ocr.documentai;

public interface DocumentAiAccessTokenProvider {

    String getAccessToken();
}
```

`DocumentAiClient.java` (기본 토큰 공급자는 내부 static `@Component` 대신 별도 빈 — 아래 `AdcAccessTokenProvider` 참조):

```java
package com.midtone.backend.ocr.documentai;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Base64;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class DocumentAiClient {

    public static class DocumentAiCallException extends RuntimeException {
        public DocumentAiCallException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private final RestClient restClient;
    private final DocumentAiAccessTokenProvider tokenProvider;
    private final String processUrl;
    private final String quotaProject;

    public DocumentAiClient(
            RestClient.Builder restClientBuilder,
            DocumentAiAccessTokenProvider tokenProvider,
            @Value("${app.documentai.endpoint}") String endpoint,
            @Value("${app.documentai.project-number}") String projectNumber,
            @Value("${app.documentai.location}") String location,
            @Value("${app.documentai.processor-id}") String processorId,
            @Value("${app.documentai.quota-project}") String quotaProject) {
        this.restClient = restClientBuilder.build();
        this.tokenProvider = tokenProvider;
        this.processUrl = "%s/v1/projects/%s/locations/%s/processors/%s:process"
                .formatted(endpoint, projectNumber, location, processorId);
        this.quotaProject = quotaProject;
    }

    public JsonNode process(byte[] image, String mimeType) {
        try {
            JsonNode response = restClient.post()
                    .uri(processUrl)
                    .header("Authorization", "Bearer " + tokenProvider.getAccessToken())
                    .header("x-goog-user-project", quotaProject)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("rawDocument", Map.of(
                            "content", Base64.getEncoder().encodeToString(image),
                            "mimeType", mimeType)))
                    .retrieve()
                    .body(JsonNode.class);
            return response == null ? com.fasterxml.jackson.databind.node.MissingNode.getInstance()
                    : response.path("document");
        } catch (RestClientException e) {
            throw new DocumentAiCallException("Document AI 호출에 실패했습니다.", e);
        }
    }
}
```

`AdcAccessTokenProvider.java` (같은 패키지에 Create — Files 목록에 추가):

```java
package com.midtone.backend.ocr.documentai;

import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import org.springframework.stereotype.Component;

@Component
public class AdcAccessTokenProvider implements DocumentAiAccessTokenProvider {

    private static final String CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";

    @Override
    public String getAccessToken() {
        try {
            GoogleCredentials credentials =
                    GoogleCredentials.getApplicationDefault().createScoped(CLOUD_PLATFORM_SCOPE);
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "ADC 자격증명을 찾을 수 없습니다. gcloud auth application-default login을 실행하세요.", e);
        }
    }
}
```

- [ ] **Step 5: 테스트 통과 확인** — 같은 명령 → PASS
- [ ] **Step 6: 커밋** — `feat : Document AI 프로세서 호출 클라이언트 추가`

---

### Task 5: OcrProcessingWorker + AsyncConfig + 기동 시 PROCESSING 정리

**Files:**
- Create: `src/main/java/com/midtone/backend/global/config/AsyncConfig.java`
- Create: `src/main/java/com/midtone/backend/ocr/application/OcrProcessingWorker.java`
- Create: `src/main/java/com/midtone/backend/ocr/application/OcrJobStartupCleaner.java`
- Test: `src/test/java/com/midtone/backend/ocr/application/OcrProcessingWorkerTest.java`
- Test: `src/test/java/com/midtone/backend/ocr/application/OcrJobStartupCleanerTest.java`

**Interfaces:**
- Consumes: `DocumentAiClient.process`, `OcrDraftParser.parse`, Task 1 도메인
- Produces: `OcrProcessingWorker.processAsync(Long jobId)`(@Async 진입점)와 `process(Long jobId)`(동기, 테스트용). 처리 규칙: PROCESSING 마킹 → Document AI 호출 → 파싱 → 기존 초안 삭제 후 저장 → COMPLETED. 초안 0건 또는 예외 시 FAILED + `error_message`.

- [ ] **Step 1: AsyncConfig 작성**

```java
package com.midtone.backend.global.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "ocrExecutor")
    public Executor ocrExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("ocr-");
        executor.initialize();
        return executor;
    }
}
```

- [ ] **Step 2: 실패하는 워커 테스트 작성** — `OcrProcessingWorkerTest.java` (Mockito, `process` 동기 호출로 검증)

```java
package com.midtone.backend.ocr.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.midtone.backend.ocr.documentai.DocumentAiClient;
import com.midtone.backend.ocr.domain.OcrDraftShiftRepository;
import com.midtone.backend.ocr.domain.OcrJob;
import com.midtone.backend.ocr.domain.OcrJobRepository;
import com.midtone.backend.ocr.domain.OcrJobStatus;
import com.midtone.backend.shift.domain.ShiftType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OcrProcessingWorkerTest {

    private OcrJobRepository ocrJobRepository;
    private OcrDraftShiftRepository ocrDraftShiftRepository;
    private DocumentAiClient documentAiClient;
    private OcrDraftParser ocrDraftParser;
    private OcrProcessingWorker worker;
    private OcrJob job;

    @BeforeEach
    void setUp() throws Exception {
        ocrJobRepository = mock(OcrJobRepository.class);
        ocrDraftShiftRepository = mock(OcrDraftShiftRepository.class);
        documentAiClient = mock(DocumentAiClient.class);
        ocrDraftParser = mock(OcrDraftParser.class);
        worker = new OcrProcessingWorker(
                ocrJobRepository, ocrDraftShiftRepository, documentAiClient, ocrDraftParser);
        job = new OcrJob(1L, new byte[] {1}, "image/png", "2026-08");
        given(ocrJobRepository.findById(10L)).willReturn(Optional.of(job));
        given(documentAiClient.process(any(), any()))
                .willReturn(new ObjectMapper().readTree("{\"text\":\"\"}"));
    }

    @Test
    void 초안을_저장하고_COMPLETED로_바꾼다() {
        given(ocrDraftParser.parse(any(), any())).willReturn(List.of(
                new OcrDraftParser.ParsedDraft(LocalDate.of(2026, 8, 1), ShiftType.DAY, new BigDecimal("0.970"))));

        worker.process(10L);

        verify(ocrDraftShiftRepository).deleteByJobId(10L);
        verify(ocrDraftShiftRepository).saveAll(anyList());
        assertEquals(OcrJobStatus.COMPLETED, job.getStatus());
    }

    @Test
    void 초안이_없으면_FAILED_처리한다() {
        given(ocrDraftParser.parse(any(), any())).willReturn(List.of());

        worker.process(10L);

        assertEquals(OcrJobStatus.FAILED, job.getStatus());
        assertEquals("근무표를 인식하지 못했습니다.", job.getErrorMessage());
    }

    @Test
    void 파싱_시_targetMonth를_YearMonth로_전달한다() {
        given(ocrDraftParser.parse(any(), any())).willReturn(List.of());
        worker.process(10L);
        verify(ocrDraftParser).parse(any(), org.mockito.ArgumentMatchers.eq(YearMonth.of(2026, 8)));
    }

    @Test
    void Document_AI_호출이_실패하면_FAILED_처리한다() {
        given(documentAiClient.process(any(), any()))
                .willThrow(new DocumentAiClient.DocumentAiCallException("boom", null));

        worker.process(10L);

        assertEquals(OcrJobStatus.FAILED, job.getStatus());
        assertEquals("Document AI 호출에 실패했습니다.", job.getErrorMessage());
    }
}
```

- [ ] **Step 3: 테스트 실패 확인** — `./gradlew test --tests "com.midtone.backend.ocr.application.OcrProcessingWorkerTest"`
- [ ] **Step 4: 워커 구현**

```java
package com.midtone.backend.ocr.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.midtone.backend.ocr.documentai.DocumentAiClient;
import com.midtone.backend.ocr.domain.OcrDraftShift;
import com.midtone.backend.ocr.domain.OcrDraftShiftRepository;
import com.midtone.backend.ocr.domain.OcrJob;
import com.midtone.backend.ocr.domain.OcrJobRepository;
import java.time.YearMonth;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OcrProcessingWorker {

    private static final String UNRECOGNIZED_MESSAGE = "근무표를 인식하지 못했습니다.";
    private static final String CALL_FAILED_MESSAGE = "Document AI 호출에 실패했습니다.";
    private static final Logger log = LoggerFactory.getLogger(OcrProcessingWorker.class);

    private final OcrJobRepository ocrJobRepository;
    private final OcrDraftShiftRepository ocrDraftShiftRepository;
    private final DocumentAiClient documentAiClient;
    private final OcrDraftParser ocrDraftParser;

    public OcrProcessingWorker(
            OcrJobRepository ocrJobRepository,
            OcrDraftShiftRepository ocrDraftShiftRepository,
            DocumentAiClient documentAiClient,
            OcrDraftParser ocrDraftParser) {
        this.ocrJobRepository = ocrJobRepository;
        this.ocrDraftShiftRepository = ocrDraftShiftRepository;
        this.documentAiClient = documentAiClient;
        this.ocrDraftParser = ocrDraftParser;
    }

    @Async("ocrExecutor")
    public void processAsync(Long jobId) {
        process(jobId);
    }

    @Transactional
    public void process(Long jobId) {
        OcrJob job = ocrJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("존재하지 않는 OCR 잡을 처리하려 했습니다. jobId={}", jobId);
            return;
        }
        job.markProcessing();
        ocrJobRepository.save(job);
        try {
            JsonNode document = documentAiClient.process(job.getImage(), job.getImageMime());
            List<OcrDraftParser.ParsedDraft> parsed =
                    ocrDraftParser.parse(document, YearMonth.parse(job.getTargetMonth()));
            if (parsed.isEmpty()) {
                job.markFailed(UNRECOGNIZED_MESSAGE);
            } else {
                ocrDraftShiftRepository.deleteByJobId(jobId);
                ocrDraftShiftRepository.saveAll(parsed.stream()
                        .map(draft -> new OcrDraftShift(jobId, draft.workDate(), draft.shiftType(), draft.confidence()))
                        .toList());
                job.markCompleted();
            }
        } catch (DocumentAiClient.DocumentAiCallException e) {
            log.error("Document AI 호출 실패. jobId={}", jobId, e);
            job.markFailed(CALL_FAILED_MESSAGE);
        } catch (RuntimeException e) {
            log.error("OCR 처리 중 예상하지 못한 오류. jobId={}", jobId, e);
            job.markFailed(UNRECOGNIZED_MESSAGE);
        }
        ocrJobRepository.save(job);
    }
}
```

- [ ] **Step 5: 워커 테스트 통과 확인**
- [ ] **Step 6: 기동 정리 테스트 작성** — `OcrJobStartupCleanerTest.java`

```java
package com.midtone.backend.ocr.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.midtone.backend.ocr.domain.OcrJob;
import com.midtone.backend.ocr.domain.OcrJobRepository;
import com.midtone.backend.ocr.domain.OcrJobStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class OcrJobStartupCleanerTest {

    @Test
    void 기동_시_PROCESSING_잡을_FAILED로_정리한다() throws Exception {
        OcrJobRepository repository = mock(OcrJobRepository.class);
        OcrJob stuck = new OcrJob(1L, new byte[] {1}, "image/png", "2026-08");
        stuck.markProcessing();
        given(repository.findByStatus(OcrJobStatus.PROCESSING)).willReturn(List.of(stuck));

        new OcrJobStartupCleaner(repository).run(null);

        assertEquals(OcrJobStatus.FAILED, stuck.getStatus());
        assertEquals("서버 재시작으로 분석이 중단되었습니다. 다시 시도해 주세요.", stuck.getErrorMessage());
    }
}
```

- [ ] **Step 7: 기동 정리 구현**

```java
package com.midtone.backend.ocr.application;

import com.midtone.backend.ocr.domain.OcrJob;
import com.midtone.backend.ocr.domain.OcrJobRepository;
import com.midtone.backend.ocr.domain.OcrJobStatus;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OcrJobStartupCleaner implements ApplicationRunner {

    private static final String INTERRUPTED_MESSAGE = "서버 재시작으로 분석이 중단되었습니다. 다시 시도해 주세요.";

    private final OcrJobRepository ocrJobRepository;

    public OcrJobStartupCleaner(OcrJobRepository ocrJobRepository) {
        this.ocrJobRepository = ocrJobRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<OcrJob> stuckJobs = ocrJobRepository.findByStatus(OcrJobStatus.PROCESSING);
        stuckJobs.forEach(job -> job.markFailed(INTERRUPTED_MESSAGE));
        ocrJobRepository.saveAll(stuckJobs);
    }
}
```

- [ ] **Step 8: 두 테스트 모두 통과 확인** — `./gradlew test --tests "com.midtone.backend.ocr.application.*"`
- [ ] **Step 9: 커밋** — `feat : OCR 비동기 처리 워커와 기동 시 잔여 잡 정리 추가`

---

### Task 6: OcrJobService (업로드·조회·보정·확정·재시도) + DTO

**Files:**
- Create: `src/main/java/com/midtone/backend/ocr/application/OcrJobService.java`
- Create: `src/main/java/com/midtone/backend/ocr/application/OcrJobResponse.java`
- Create: `src/main/java/com/midtone/backend/ocr/application/OcrJobDetailResponse.java`
- Create: `src/main/java/com/midtone/backend/ocr/application/OcrDraftResponse.java`
- Create: `src/main/java/com/midtone/backend/ocr/application/UpdateOcrDraftRequest.java`
- Create: `src/main/java/com/midtone/backend/ocr/application/ConfirmOcrJobResponse.java`
- Modify: `src/main/java/com/midtone/backend/shift/domain/ShiftSchedule.java` — OCR용 정적 팩토리 추가
- Test: `src/test/java/com/midtone/backend/ocr/application/OcrJobServiceTest.java`

**Interfaces:**
- Consumes: Task 1 도메인, Task 2 예외, Task 5 워커(`processAsync`), `CurrentUserIdProvider.getCurrentUserId()`, `ShiftScheduleRepository`(`findByUserIdAndWorkDateBetweenOrderByWorkDateAsc`, `existsByUserIdAndWorkDate` 등 기존 메서드 + 아래 추가), `ShiftCoachingRegenerationTrigger.triggerForRange(LocalDate, LocalDate)`
- Produces:
  - `OcrJobResponse(Long jobId, String status)`
  - `OcrDraftResponse(Long draftId, String workDate, String shiftType, String startTime, String endTime, BigDecimal confidence, boolean excluded)` — 시간 null이면 null 직렬화
  - `OcrJobDetailResponse(Long jobId, String status, String targetMonth, String errorMessage, List<OcrDraftResponse> drafts)` — COMPLETED가 아니면 drafts는 빈 리스트
  - `UpdateOcrDraftRequest(String workDate, String shiftType, String startTime, String endTime, Boolean excluded)` (모두 optional)
  - `ConfirmOcrJobResponse(int confirmedCount, List<String> replacedDates, List<String> affectedCoachingDates)`
  - `ShiftSchedule.fromOcr(Long userId, LocalDate workDate, ShiftType shiftType, ShiftTime shiftTime, BigDecimal confidence)` — source=OCR, confirmed=true
  - 서비스 메서드: `upload(MultipartFile image, String month)`, `getJob(Long jobId)`, `updateDraft(Long jobId, Long draftId, UpdateOcrDraftRequest)`, `confirm(Long jobId)`, `retry(Long jobId)`
- `ShiftScheduleRepository`에 추가: `Optional<ShiftSchedule> findByUserIdAndWorkDate(Long userId, LocalDate workDate);`

- [ ] **Step 1: ShiftSchedule에 OCR 팩토리 추가** — `ShiftSchedule.java`의 public 생성자 아래:

```java
public static ShiftSchedule fromOcr(
        Long userId, LocalDate workDate, ShiftType shiftType, ShiftTime shiftTime, BigDecimal confidence) {
    ShiftSchedule shift = new ShiftSchedule(userId, workDate, shiftType, shiftTime);
    shift.source = ShiftSource.OCR;
    shift.confidence = confidence;
    return shift;
}
```

- [ ] **Step 2: 실패하는 서비스 테스트 작성** — `OcrJobServiceTest.java` (핵심 경로만 발췌 — 나머지 검증 규칙도 같은 패턴으로 케이스 추가)

```java
package com.midtone.backend.ocr.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.ocr.domain.OcrDraftShift;
import com.midtone.backend.ocr.domain.OcrDraftShiftRepository;
import com.midtone.backend.ocr.domain.OcrJob;
import com.midtone.backend.ocr.domain.OcrJobRepository;
import com.midtone.backend.ocr.domain.OcrJobStatus;
import com.midtone.backend.shift.application.schedule.ShiftCoachingRegenerationTrigger;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class OcrJobServiceTest {

    private OcrJobRepository ocrJobRepository;
    private OcrDraftShiftRepository ocrDraftShiftRepository;
    private ShiftScheduleRepository shiftScheduleRepository;
    private OcrProcessingWorker worker;
    private ShiftCoachingRegenerationTrigger coachingTrigger;
    private OcrJobService service;

    @BeforeEach
    void setUp() {
        ocrJobRepository = mock(OcrJobRepository.class);
        ocrDraftShiftRepository = mock(OcrDraftShiftRepository.class);
        shiftScheduleRepository = mock(ShiftScheduleRepository.class);
        worker = mock(OcrProcessingWorker.class);
        coachingTrigger = mock(ShiftCoachingRegenerationTrigger.class);
        CurrentUserIdProvider userIdProvider = () -> 1L;
        service = new OcrJobService(
                ocrJobRepository, ocrDraftShiftRepository, shiftScheduleRepository,
                worker, coachingTrigger, userIdProvider);
        given(ocrJobRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
    }

    private OcrJob completedJob() {
        OcrJob job = new OcrJob(1L, new byte[] {1}, "image/png", "2026-08");
        job.markProcessing();
        job.markCompleted();
        return job;
    }

    @Test
    void 업로드하면_잡을_저장하고_비동기_처리를_시작한다() {
        MockMultipartFile image = new MockMultipartFile("image", "roster.png", "image/png", new byte[] {1, 2});

        OcrJobResponse response = service.upload(image, "2026-08");

        assertEquals("PENDING", response.status());
        verify(worker).processAsync(any());
    }

    @Test
    void 지원하지_않는_이미지_형식이면_400_예외를_던진다() {
        MockMultipartFile image = new MockMultipartFile("image", "roster.gif", "image/gif", new byte[] {1});
        OcrException exception = assertThrows(OcrException.class, () -> service.upload(image, "2026-08"));
        assertEquals(OcrException.ErrorCode.UNSUPPORTED_IMAGE_TYPE, exception.getErrorCode());
    }

    @Test
    void month가_형식에_안_맞으면_400_예외를_던진다() {
        MockMultipartFile image = new MockMultipartFile("image", "roster.png", "image/png", new byte[] {1});
        OcrException exception = assertThrows(OcrException.class, () -> service.upload(image, "2026/08"));
        assertEquals(OcrException.ErrorCode.INVALID_MONTH, exception.getErrorCode());
    }

    @Test
    void 다른_사용자의_잡을_조회하면_403_예외를_던진다() {
        OcrJob othersJob = new OcrJob(2L, new byte[] {1}, "image/png", "2026-08");
        given(ocrJobRepository.findById(10L)).willReturn(Optional.of(othersJob));
        OcrException exception = assertThrows(OcrException.class, () -> service.getJob(10L));
        assertEquals(OcrException.ErrorCode.JOB_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void COMPLETED가_아니면_확정_시_409_예외를_던진다() {
        OcrJob pending = new OcrJob(1L, new byte[] {1}, "image/png", "2026-08");
        given(ocrJobRepository.findById(10L)).willReturn(Optional.of(pending));
        OcrException exception = assertThrows(OcrException.class, () -> service.confirm(10L));
        assertEquals(OcrException.ErrorCode.JOB_NOT_COMPLETED, exception.getErrorCode());
    }

    @Test
    void 확정하면_초안을_일정으로_반영하고_겹치는_날짜는_덮어쓴다() {
        OcrJob job = completedJob();
        given(ocrJobRepository.findById(10L)).willReturn(Optional.of(job));
        OcrDraftShift draft = new OcrDraftShift(10L, LocalDate.of(2026, 8, 1), ShiftType.DAY, new BigDecimal("0.970"));
        given(ocrDraftShiftRepository.findByJobIdOrderByWorkDateAsc(any())).willReturn(List.of(draft));
        ShiftSchedule existing = new ShiftSchedule(
                1L, LocalDate.of(2026, 8, 1), ShiftType.NIGHT, new ShiftTime(null, null));
        given(shiftScheduleRepository.findByUserIdAndWorkDate(1L, LocalDate.of(2026, 8, 1)))
                .willReturn(Optional.of(existing));
        given(coachingTrigger.triggerForRange(any(), any())).willReturn(List.of("2026-08-01"));

        ConfirmOcrJobResponse response = service.confirm(10L);

        assertEquals(1, response.confirmedCount());
        assertEquals(List.of("2026-08-01"), response.replacedDates());
        assertEquals(List.of("2026-08-01"), response.affectedCoachingDates());
        verify(shiftScheduleRepository).delete(existing);
        verify(shiftScheduleRepository).saveAll(anyIterable());
        assertEquals(OcrJobStatus.CONFIRMED, job.getStatus());
    }

    @Test
    void excluded_초안은_확정에서_제외한다() {
        OcrJob job = completedJob();
        given(ocrJobRepository.findById(10L)).willReturn(Optional.of(job));
        OcrDraftShift draft = new OcrDraftShift(10L, LocalDate.of(2026, 8, 1), ShiftType.DAY, null);
        draft.applyCorrection(null, null, null, null, true);
        given(ocrDraftShiftRepository.findByJobIdOrderByWorkDateAsc(any())).willReturn(List.of(draft));
        given(coachingTrigger.triggerForRange(any(), any())).willReturn(List.of());

        ConfirmOcrJobResponse response = service.confirm(10L);

        assertEquals(0, response.confirmedCount());
    }

    @Test
    void PENDING_잡은_재시도할_수_없다() {
        OcrJob pending = new OcrJob(1L, new byte[] {1}, "image/png", "2026-08");
        given(ocrJobRepository.findById(10L)).willReturn(Optional.of(pending));
        OcrException exception = assertThrows(OcrException.class, () -> service.retry(10L));
        assertEquals(OcrException.ErrorCode.JOB_NOT_RETRYABLE, exception.getErrorCode());
    }

    private static Iterable<ShiftSchedule> anyIterable() {
        return org.mockito.ArgumentMatchers.anyIterable();
    }
}
```

추가 케이스(같은 패턴으로 함께 작성): `getJob`은 COMPLETED면 초안 목록 포함, `updateDraft`는 COMPLETED가 아니면 409·jobId 불일치 초안은 404·수정 후 `OcrDraftResponse` 반환, `retry`는 FAILED 잡이면 PENDING으로 되돌리고 `worker.processAsync` 재호출, 이미지 비어 있으면 `IMAGE_REQUIRED`, 10MB 초과면 `IMAGE_TOO_LARGE`, month가 null이면 현재 달 사용(`DateTimeDefaults` 참고 — `global/time/DateTimeDefaults.java`에 기본 타임존이 있으면 그것을 사용, 없으면 `LocalDate.now()` 기반).

- [ ] **Step 3: 테스트 실패 확인** — `./gradlew test --tests "com.midtone.backend.ocr.application.OcrJobServiceTest"`
- [ ] **Step 4: DTO 구현** — 각 record:

```java
package com.midtone.backend.ocr.application;

public record OcrJobResponse(Long jobId, String status) {
}
```

```java
package com.midtone.backend.ocr.application;

import com.midtone.backend.ocr.domain.OcrDraftShift;
import java.math.BigDecimal;

public record OcrDraftResponse(
        Long draftId, String workDate, String shiftType,
        String startTime, String endTime, BigDecimal confidence, boolean excluded) {

    public static OcrDraftResponse from(OcrDraftShift draft) {
        return new OcrDraftResponse(
                draft.getId(),
                draft.getWorkDate().toString(),
                draft.getShiftType().name(),
                draft.getStartTime() == null ? null : draft.getStartTime().toString(),
                draft.getEndTime() == null ? null : draft.getEndTime().toString(),
                draft.getConfidence(),
                draft.isExcluded());
    }
}
```

```java
package com.midtone.backend.ocr.application;

import com.midtone.backend.ocr.domain.OcrJob;
import java.util.List;

public record OcrJobDetailResponse(
        Long jobId, String status, String targetMonth, String errorMessage, List<OcrDraftResponse> drafts) {

    public static OcrJobDetailResponse from(OcrJob job, List<OcrDraftResponse> drafts) {
        return new OcrJobDetailResponse(
                job.getId(), job.getStatus().name(), job.getTargetMonth(), job.getErrorMessage(), drafts);
    }
}
```

```java
package com.midtone.backend.ocr.application;

public record UpdateOcrDraftRequest(
        String workDate, String shiftType, String startTime, String endTime, Boolean excluded) {
}
```

```java
package com.midtone.backend.ocr.application;

import java.util.List;

public record ConfirmOcrJobResponse(
        int confirmedCount, List<String> replacedDates, List<String> affectedCoachingDates) {
}
```

- [ ] **Step 5: ShiftScheduleRepository에 조회 메서드 추가** — `Optional<ShiftSchedule> findByUserIdAndWorkDate(Long userId, LocalDate workDate);`
- [ ] **Step 6: 서비스 구현**

```java
package com.midtone.backend.ocr.application;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.ocr.domain.OcrDraftShift;
import com.midtone.backend.ocr.domain.OcrDraftShiftRepository;
import com.midtone.backend.ocr.domain.OcrJob;
import com.midtone.backend.ocr.domain.OcrJobRepository;
import com.midtone.backend.ocr.domain.OcrJobStatus;
import com.midtone.backend.shift.application.schedule.ShiftCoachingRegenerationTrigger;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OcrJobService {

    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of("image/jpeg", "image/png");
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    private final OcrJobRepository ocrJobRepository;
    private final OcrDraftShiftRepository ocrDraftShiftRepository;
    private final ShiftScheduleRepository shiftScheduleRepository;
    private final OcrProcessingWorker ocrProcessingWorker;
    private final ShiftCoachingRegenerationTrigger shiftCoachingRegenerationTrigger;
    private final CurrentUserIdProvider currentUserIdProvider;

    public OcrJobService(
            OcrJobRepository ocrJobRepository,
            OcrDraftShiftRepository ocrDraftShiftRepository,
            ShiftScheduleRepository shiftScheduleRepository,
            OcrProcessingWorker ocrProcessingWorker,
            ShiftCoachingRegenerationTrigger shiftCoachingRegenerationTrigger,
            CurrentUserIdProvider currentUserIdProvider) {
        this.ocrJobRepository = ocrJobRepository;
        this.ocrDraftShiftRepository = ocrDraftShiftRepository;
        this.shiftScheduleRepository = shiftScheduleRepository;
        this.ocrProcessingWorker = ocrProcessingWorker;
        this.shiftCoachingRegenerationTrigger = shiftCoachingRegenerationTrigger;
        this.currentUserIdProvider = currentUserIdProvider;
    }

    public OcrJobResponse upload(MultipartFile image, String month) {
        long userId = currentUserIdProvider.getCurrentUserId();
        validateImage(image);
        String targetMonth = resolveMonth(month);
        byte[] content = readBytes(image);
        OcrJob job = ocrJobRepository.save(new OcrJob(userId, content, image.getContentType(), targetMonth));
        ocrProcessingWorker.processAsync(job.getId());
        return new OcrJobResponse(job.getId(), job.getStatus().name());
    }

    @Transactional(readOnly = true)
    public OcrJobDetailResponse getJob(Long jobId) {
        OcrJob job = findOwnedJob(jobId);
        List<OcrDraftResponse> drafts = job.getStatus() == OcrJobStatus.COMPLETED
                ? ocrDraftShiftRepository.findByJobIdOrderByWorkDateAsc(jobId).stream()
                        .map(OcrDraftResponse::from)
                        .toList()
                : List.of();
        return OcrJobDetailResponse.from(job, drafts);
    }

    @Transactional
    public OcrDraftResponse updateDraft(Long jobId, Long draftId, UpdateOcrDraftRequest request) {
        OcrJob job = findOwnedJob(jobId);
        requireStatus(job, OcrJobStatus.COMPLETED, OcrException.ErrorCode.JOB_NOT_COMPLETED);
        OcrDraftShift draft = ocrDraftShiftRepository.findById(draftId)
                .filter(found -> found.getJobId().equals(jobId))
                .orElseThrow(() -> new OcrException(OcrException.ErrorCode.DRAFT_NOT_FOUND));
        draft.applyCorrection(
                request.workDate() == null ? null : LocalDate.parse(request.workDate()),
                request.shiftType() == null ? null : ShiftType.valueOf(request.shiftType()),
                request.startTime() == null ? null : LocalTime.parse(request.startTime()),
                request.endTime() == null ? null : LocalTime.parse(request.endTime()),
                request.excluded());
        return OcrDraftResponse.from(draft);
    }

    @Transactional
    public ConfirmOcrJobResponse confirm(Long jobId) {
        OcrJob job = findOwnedJob(jobId);
        requireStatus(job, OcrJobStatus.COMPLETED, OcrException.ErrorCode.JOB_NOT_COMPLETED);
        long userId = job.getUserId();
        List<OcrDraftShift> drafts = ocrDraftShiftRepository.findByJobIdOrderByWorkDateAsc(jobId).stream()
                .filter(draft -> !draft.isExcluded())
                .toList();
        List<String> replacedDates = new ArrayList<>();
        List<ShiftSchedule> newShifts = new ArrayList<>();
        for (OcrDraftShift draft : drafts) {
            shiftScheduleRepository.findByUserIdAndWorkDate(userId, draft.getWorkDate()).ifPresent(existing -> {
                shiftScheduleRepository.delete(existing);
                replacedDates.add(draft.getWorkDate().toString());
            });
            newShifts.add(ShiftSchedule.fromOcr(
                    userId, draft.getWorkDate(), draft.getShiftType(),
                    new ShiftTime(draft.getStartTime(), draft.getEndTime()), draft.getConfidence()));
        }
        shiftScheduleRepository.flush();
        shiftScheduleRepository.saveAll(newShifts);
        List<String> affectedCoachingDates = List.of();
        if (!drafts.isEmpty()) {
            affectedCoachingDates = shiftCoachingRegenerationTrigger.triggerForRange(
                    drafts.get(0).getWorkDate(), drafts.get(drafts.size() - 1).getWorkDate());
        }
        job.markConfirmed();
        ocrJobRepository.save(job);
        return new ConfirmOcrJobResponse(newShifts.size(), replacedDates, affectedCoachingDates);
    }

    @Transactional
    public OcrJobResponse retry(Long jobId) {
        OcrJob job = findOwnedJob(jobId);
        if (job.getStatus() != OcrJobStatus.FAILED && job.getStatus() != OcrJobStatus.COMPLETED) {
            throw new OcrException(OcrException.ErrorCode.JOB_NOT_RETRYABLE);
        }
        ocrProcessingWorker.processAsync(jobId);
        return new OcrJobResponse(jobId, OcrJobStatus.PROCESSING.name());
    }

    private OcrJob findOwnedJob(Long jobId) {
        OcrJob job = ocrJobRepository.findById(jobId)
                .orElseThrow(() -> new OcrException(OcrException.ErrorCode.JOB_NOT_FOUND));
        if (!job.isOwnedBy(currentUserIdProvider.getCurrentUserId())) {
            throw new OcrException(OcrException.ErrorCode.JOB_ACCESS_DENIED);
        }
        return job;
    }

    private void requireStatus(OcrJob job, OcrJobStatus expected, OcrException.ErrorCode errorCode) {
        if (job.getStatus() != expected) {
            throw new OcrException(errorCode);
        }
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new OcrException(OcrException.ErrorCode.IMAGE_REQUIRED);
        }
        if (!SUPPORTED_MIME_TYPES.contains(image.getContentType())) {
            throw new OcrException(OcrException.ErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }
        if (image.getSize() > MAX_IMAGE_BYTES) {
            throw new OcrException(OcrException.ErrorCode.IMAGE_TOO_LARGE);
        }
    }

    private String resolveMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now().toString();
        }
        try {
            return YearMonth.parse(month).toString();
        } catch (DateTimeParseException e) {
            throw new OcrException(OcrException.ErrorCode.INVALID_MONTH);
        }
    }

    private byte[] readBytes(MultipartFile image) {
        try {
            return image.getBytes();
        } catch (java.io.IOException e) {
            throw new OcrException(OcrException.ErrorCode.IMAGE_REQUIRED);
        }
    }
}
```

구현 참고: `resolveMonth`의 `YearMonth.now()`는 `global/time/DateTimeDefaults.java`에 기본 ZoneId 상수가 있으면 `YearMonth.now(DateTimeDefaults.<상수>)`로 교체한다(파일 열어 확인). `retry`에서 상태는 워커가 PROCESSING으로 바꾸므로 응답만 PROCESSING으로 안내한다. `confirm`의 `flush()`는 delete가 insert보다 먼저 DB에 반영되어 유니크 제약 위반을 피하기 위함이다.

- [ ] **Step 7: 테스트 통과 확인** — `./gradlew test --tests "com.midtone.backend.ocr.application.OcrJobServiceTest"` → PASS
- [ ] **Step 8: 커밋** — `feat : OCR 잡 업로드·검수·확정·재시도 서비스 추가`

---

### Task 7: OcrController

**Files:**
- Create: `src/main/java/com/midtone/backend/ocr/OcrController.java`
- Test: `src/test/java/com/midtone/backend/ocr/OcrControllerTest.java`

**Interfaces:**
- Consumes: Task 6 서비스·DTO 전부
- Produces: `POST /api/v1/ocr/jobs`(multipart `image` + optional `month`, 202), `GET /api/v1/ocr/jobs/{jobId}`(200), `PATCH /api/v1/ocr/jobs/{jobId}/drafts/{draftId}`(200), `POST /api/v1/ocr/jobs/{jobId}:confirm`(200), `POST /api/v1/ocr/jobs/{jobId}:retry`(202)

- [ ] **Step 1: 실패하는 컨트롤러 테스트 작성** — 기존 `ShiftControllerTest` 스타일(`@WebMvcTest(OcrController.class)` + `@AutoConfigureMockMvc(addFilters = false)` + `@MockitoBean OcrJobService`):

```java
package com.midtone.backend.ocr;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.ocr.application.ConfirmOcrJobResponse;
import com.midtone.backend.ocr.application.OcrDraftResponse;
import com.midtone.backend.ocr.application.OcrException;
import com.midtone.backend.ocr.application.OcrJobDetailResponse;
import com.midtone.backend.ocr.application.OcrJobResponse;
import com.midtone.backend.ocr.application.OcrJobService;
import com.midtone.backend.ocr.application.UpdateOcrDraftRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OcrController.class)
@AutoConfigureMockMvc(addFilters = false)
class OcrControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OcrJobService ocrJobService;

    @Test
    void 이미지를_업로드하면_202와_잡_ID를_반환한다() throws Exception {
        given(ocrJobService.upload(any(), eq("2026-08"))).willReturn(new OcrJobResponse(1L, "PENDING"));

        mockMvc.perform(multipart("/api/v1/ocr/jobs")
                        .file(new MockMultipartFile("image", "roster.png", "image/png", new byte[] {1}))
                        .param("month", "2026-08"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void 잡_상태를_조회한다() throws Exception {
        given(ocrJobService.getJob(1L)).willReturn(new OcrJobDetailResponse(
                1L, "COMPLETED", "2026-08", null,
                List.of(new OcrDraftResponse(5L, "2026-08-01", "DAY", null, null, new BigDecimal("0.970"), false))));

        mockMvc.perform(get("/api/v1/ocr/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.drafts[0].draftId").value(5))
                .andExpect(jsonPath("$.drafts[0].shiftType").value("DAY"));
    }

    @Test
    void 초안을_보정한다() throws Exception {
        given(ocrJobService.updateDraft(eq(1L), eq(5L), any(UpdateOcrDraftRequest.class)))
                .willReturn(new OcrDraftResponse(5L, "2026-08-01", "NIGHT", null, null, new BigDecimal("0.970"), false));

        mockMvc.perform(patch("/api/v1/ocr/jobs/1/drafts/5")
                        .contentType("application/json")
                        .content("{\"shiftType\":\"NIGHT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shiftType").value("NIGHT"));
    }

    @Test
    void 확정하면_반영_결과를_반환한다() throws Exception {
        given(ocrJobService.confirm(1L)).willReturn(
                new ConfirmOcrJobResponse(3, List.of("2026-08-01"), List.of("2026-08-01", "2026-08-02")));

        mockMvc.perform(post("/api/v1/ocr/jobs/1:confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedCount").value(3))
                .andExpect(jsonPath("$.replacedDates[0]").value("2026-08-01"));
    }

    @Test
    void 재시도하면_202를_반환한다() throws Exception {
        given(ocrJobService.retry(1L)).willReturn(new OcrJobResponse(1L, "PROCESSING"));

        mockMvc.perform(post("/api/v1/ocr/jobs/1:retry"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    void 완료되지_않은_잡을_확정하면_409를_반환한다() throws Exception {
        given(ocrJobService.confirm(anyLong()))
                .willThrow(new OcrException(OcrException.ErrorCode.JOB_NOT_COMPLETED));

        mockMvc.perform(post("/api/v1/ocr/jobs/1:confirm"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("분석이 완료된 작업만 검수·확정할 수 있습니다."));
    }
}
```

- [ ] **Step 2: 테스트 실패 확인** — `./gradlew test --tests "com.midtone.backend.ocr.OcrControllerTest"`
- [ ] **Step 3: 컨트롤러 구현**

```java
package com.midtone.backend.ocr;

import com.midtone.backend.ocr.application.ConfirmOcrJobResponse;
import com.midtone.backend.ocr.application.OcrDraftResponse;
import com.midtone.backend.ocr.application.OcrJobDetailResponse;
import com.midtone.backend.ocr.application.OcrJobResponse;
import com.midtone.backend.ocr.application.OcrJobService;
import com.midtone.backend.ocr.application.UpdateOcrDraftRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class OcrController {

    private final OcrJobService ocrJobService;

    public OcrController(OcrJobService ocrJobService) {
        this.ocrJobService = ocrJobService;
    }

    @PostMapping(value = "/ocr/jobs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OcrJobResponse> uploadJob(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "month", required = false) String month) {
        OcrJobResponse response = ocrJobService.upload(image, month);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/ocr/jobs/{jobId}")
    public ResponseEntity<OcrJobDetailResponse> getJob(@PathVariable Long jobId) {
        return ResponseEntity.ok(ocrJobService.getJob(jobId));
    }

    @PatchMapping("/ocr/jobs/{jobId}/drafts/{draftId}")
    public ResponseEntity<OcrDraftResponse> updateDraft(
            @PathVariable Long jobId, @PathVariable Long draftId, @RequestBody UpdateOcrDraftRequest request) {
        return ResponseEntity.ok(ocrJobService.updateDraft(jobId, draftId, request));
    }

    @PostMapping("/ocr/jobs/{jobId}:confirm")
    public ResponseEntity<ConfirmOcrJobResponse> confirmJob(@PathVariable Long jobId) {
        return ResponseEntity.ok(ocrJobService.confirm(jobId));
    }

    @PostMapping("/ocr/jobs/{jobId}:retry")
    public ResponseEntity<OcrJobResponse> retryJob(@PathVariable Long jobId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ocrJobService.retry(jobId));
    }
}
```

주의: 콜론 경로(`{jobId}:confirm`)가 매핑되지 않으면 기존 커밋 `f847040`(콜론 경로 API 매핑 수정)이 손댄 설정을 확인해 같은 방식을 따른다(`git show f847040 --stat`).

- [ ] **Step 4: 테스트 통과 확인** — 같은 명령 → PASS
- [ ] **Step 5: 커밋** — `feat : OCR 일정 입력 API 엔드포인트 추가`

---

### Task 8: 인프라 설정 (docker-compose ADC 마운트) + 문서 갱신

**Files:**
- Modify: `docker-compose.yml` — backend 서비스에 ADC 마운트·환경변수
- Modify: `README.md` — API 표에 OCR 행 추가, 구현 현황에 P5 추가, ADC 로그인 사전 조건 추가
- Modify: `docs/backend-priority.md` — P5 상태 갱신

- [ ] **Step 1: docker-compose.yml 수정** — `backend` 서비스 `environment`에 추가:

```yaml
      GOOGLE_CLOUD_PROJECT: shiftmate-504210
```

`backend` 서비스에 `volumes` 추가 (Windows 호스트의 gcloud 설정 위치는 `%APPDATA%\gcloud`):

```yaml
    volumes:
      - ${GCLOUD_CONFIG_DIR:-${APPDATA}/gcloud}:/root/.config/gcloud:ro
```

Linux/macOS 팀원은 `.env`에 `GCLOUD_CONFIG_DIR=~/.config/gcloud`를 지정하도록 README에 명시.

- [ ] **Step 2: config 검증** — `docker compose --env-file .env.example config` → 오류 없음 확인 (`.env.example`에 `GCLOUD_CONFIG_DIR` 주석 추가)
- [ ] **Step 3: README.md 갱신** — API 표에 `| OCR 일정 입력 | POST /ocr/jobs, GET /ocr/jobs/{jobId}, PATCH /ocr/jobs/{jobId}/drafts/{draftId}, POST /ocr/jobs/{jobId}:confirm, POST /ocr/jobs/{jobId}:retry |` 행 추가. 구현 현황에 `**P5 OCR 일정 입력** — 이미지 업로드, Document AI 분석, 초안 검수·확정` 추가, 마지막 문장에서 P5 제거. 사전 요구사항 절에 ADC 로그인 명령(`gcloud auth application-default login --impersonate-service-account=shiftmate-vision@shiftmate-504210.iam.gserviceaccount.com`) 추가. 환경변수 표에 `DOCUMENTAI_*` 행 추가.
- [ ] **Step 4: docs/backend-priority.md 갱신** — P5 절에 `**상태: 완료**` 추가(P0 스타일과 동일).
- [ ] **Step 5: 커밋** — `chore : Document AI 연동을 위한 컨테이너 ADC 마운트 및 문서 갱신`

---

### Task 9: 통합 테스트 (Testcontainers)

**Files:**
- Test: `src/test/java/com/midtone/backend/ocr/OcrIntegrationTest.java`

**Interfaces:**
- Consumes: 전체 스택. `support.IntegrationTest` 상속, `TestUserFixture.createUserWithSettings(...)`, `JwtProvider.createAccessToken(userId)` 패턴은 `ShiftScheduleIntegrationTest` 참조. `DocumentAiClient`만 `@MockitoBean`으로 대체(실 GCP 호출 차단). `OcrProcessingWorker`는 실제 빈을 사용하되 업로드 후 폴링으로 완료를 기다린다.

- [ ] **Step 1: 통합 테스트 작성** — 시나리오:

```java
package com.midtone.backend.ocr;

// import 는 ShiftScheduleIntegrationTest 패턴 + 아래 사용 클래스 기준으로 구성

class OcrIntegrationTest extends IntegrationTest {

    // @MockitoBean DocumentAiClient documentAiClient;
    // 픽스처: Task 3의 form-parser-response.json을 ObjectMapper로 읽어 given(documentAiClient.process(any(), any()))로 반환

    // 테스트 1: 업로드→폴링→검수→확정 전체 흐름
    //  1) 사용자 생성 + 액세스 토큰 발급
    //  2) POST /api/v1/ocr/jobs (multipart image=PNG 바이트, month=2026-08) → 202, jobId 획득
    //  3) GET /api/v1/ocr/jobs/{jobId}를 최대 5초간 200ms 간격 폴링 → status COMPLETED, drafts 4건
    //  4) PATCH /api/v1/ocr/jobs/{jobId}/drafts/{draftId} 로 첫 초안 shiftType을 NIGHT로 보정 → 200
    //  5) 기존 일정 생성: POST /api/v1/shifts 로 2026-08-02에 DAY 일정 등록 (덮어쓰기 검증용)
    //  6) POST /api/v1/ocr/jobs/{jobId}:confirm → 200, confirmedCount=4, replacedDates=["2026-08-02"]
    //  7) ShiftScheduleRepository로 2026-08-01 조회 → shiftType NIGHT, source OCR 확인
    //  8) GET /api/v1/ocr/jobs/{jobId} → status CONFIRMED

    // 테스트 2: Document AI 실패 시 FAILED와 재시도
    //  1) documentAiClient.process가 DocumentAiCallException을 던지도록 스텁
    //  2) 업로드 → 폴링 → status FAILED, errorMessage 존재
    //  3) 스텁을 정상 응답으로 교체 후 POST :retry → 202
    //  4) 폴링 → COMPLETED

    // 테스트 3: 다른 사용자의 잡 접근 시 403
    //  사용자 A로 업로드, 사용자 B 토큰으로 GET → 403
}
```

폴링 헬퍼는 `org.awaitility`가 의존성에 없으므로 단순 `for` 루프 + `Thread.sleep(200)`으로 작성한다. `@BeforeEach`에서 `ocrDraftShiftRepository.deleteAll()`, `ocrJobRepository.deleteAll()`, `shiftScheduleRepository.deleteAll()`, `dailyCoachingRepository.deleteAll()` 순으로 정리한다(기존 통합 테스트와 동일 패턴).

- [ ] **Step 2: 테스트 실행** — `./gradlew test --tests "com.midtone.backend.ocr.OcrIntegrationTest"` (Docker 필요) → PASS
- [ ] **Step 3: 전체 테스트 실행** — `./gradlew test` → 전체 PASS (기존 테스트 회귀 없음 확인)
- [ ] **Step 4: 커밋** — `test : OCR 일정 입력 통합 테스트 추가`

---

### Task 10: 실 GCP E2E 스모크 (결제 연결 후, 선택적 수동 검증)

**Files:** 없음 (수동 검증)

- [ ] **Step 1: 결제 연결 확인** — Document AI `:process` 호출이 `BILLING_DISABLED`가 아닌지 curl로 확인 (스펙 "남은 외부 의존" 참조)
- [ ] **Step 2: 로컬 기동** — `docker compose up --build` 후 실제 근무표 이미지로 `POST /api/v1/ocr/jobs` → 폴링 → 초안 확인
- [ ] **Step 3: 파서 보완** — 실제 Form Parser 응답에서 초안이 비거나 어긋나면, 응답 JSON을 `src/test/resources/ocr/`에 픽스처로 추가하고 Task 3 테스트를 보강한 뒤 파서를 수정한다 (TDD 순서 유지)

## Self-Review 결과

- 스펙 전 요구사항 매핑: 업로드(T6/T7), 상태 조회(T6/T7), 보정(T6/T7), 확정·덮어쓰기·코칭 재생성(T6), 재시도(T6/T7), V7(T1), 파서(T3), Document AI 클라이언트(T4), 비동기·기동 정리(T5), 설정·문서(T8), 통합 검증(T9), 실 GCP 검증(T10) — 누락 없음.
- 타입 일관성: `ParsedDraft`(T3 정의, T5 소비), `OcrJobResponse` 등 DTO(T6 정의, T7 소비), `ShiftSchedule.fromOcr`(T6 정의·소비) 확인.
- 알려진 리스크: ① google-auth-library 버전 핀은 해석 실패 시 조정 필요(T4에 명시), ② 콜론 경로 매핑(T7에 확인 절차 명시), ③ 픽스처 문자 인덱스(T3에 검산 지침 명시).
