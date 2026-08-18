package com.midtone.backend.caffeine.domain;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.midtone.backend.global.time.DateTimeDefaults;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaffeineIntakeTest {

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
        CaffeineIntake intake = new CaffeineIntake(1L, LocalDateTime.parse("2026-08-19T00:15:00"),
                "Asia/Seoul", 120, new BigDecimal("1.00"), null);

        LocalDateTime expected = LocalDateTime.now(DateTimeDefaults.DEFAULT_ZONE);
        assertTrue(Duration.between(intake.getCreatedAt(), expected).abs().toMinutes() < 1,
                "createdAt=" + intake.getCreatedAt() + " expectedNear=" + expected);
    }

    @Test
    void update시_updatedAt도_서비스_기준_시간대로_기록한다() {
        CaffeineIntake intake = new CaffeineIntake(1L, LocalDateTime.parse("2026-08-19T00:15:00"),
                "Asia/Seoul", 120, new BigDecimal("1.00"), null);

        intake.update(LocalDateTime.parse("2026-08-19T00:20:00"), "Asia/Seoul", 150, new BigDecimal("1.00"), null);

        LocalDateTime expected = LocalDateTime.now(DateTimeDefaults.DEFAULT_ZONE);
        assertTrue(Duration.between(intake.getUpdatedAt(), expected).abs().toMinutes() < 1,
                "updatedAt=" + intake.getUpdatedAt() + " expectedNear=" + expected);
    }
}
