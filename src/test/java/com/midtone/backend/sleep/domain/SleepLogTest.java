package com.midtone.backend.sleep.domain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.midtone.backend.global.time.DateTimeDefaults;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SleepLogTest {

    private TimeZone originalTimeZone;

    @BeforeEach
    void setUp() {
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    void createdAt은_JVM_기본_존과_무관하게_서비스_기준_시간대로_기록한다() {
        SleepLog log = new SleepLog(1L,
                LocalDateTime.parse("2026-08-18T09:00:00"), LocalDateTime.parse("2026-08-18T16:00:00"),
                "Asia/Seoul", SleepLogSource.MANUAL);

        LocalDateTime expected = LocalDateTime.now(DateTimeDefaults.DEFAULT_ZONE);
        assertTrue(Duration.between(log.getCreatedAt(), expected).abs().toMinutes() < 1,
                "createdAt=" + log.getCreatedAt() + " expectedNear=" + expected);
    }

    @Test
    void update시_updatedAt도_서비스_기준_시간대로_기록한다() {
        SleepLog log = new SleepLog(1L,
                LocalDateTime.parse("2026-08-18T09:00:00"), LocalDateTime.parse("2026-08-18T16:00:00"),
                "Asia/Seoul", SleepLogSource.MANUAL);

        log.update(LocalDateTime.parse("2026-08-18T09:30:00"), LocalDateTime.parse("2026-08-18T16:00:00"),
                "Asia/Seoul", SleepLogSource.MANUAL);

        LocalDateTime expected = LocalDateTime.now(DateTimeDefaults.DEFAULT_ZONE);
        assertTrue(Duration.between(log.getUpdatedAt(), expected).abs().toMinutes() < 1,
                "updatedAt=" + log.getUpdatedAt() + " expectedNear=" + expected);
    }
}
