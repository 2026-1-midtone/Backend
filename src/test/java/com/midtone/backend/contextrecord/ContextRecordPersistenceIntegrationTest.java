package com.midtone.backend.contextrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.midtone.backend.caffeine.domain.CaffeineIntake;
import com.midtone.backend.caffeine.domain.CaffeineIntakeRepository;
import com.midtone.backend.sleep.domain.SleepLog;
import com.midtone.backend.sleep.domain.SleepLogRepository;
import com.midtone.backend.sleep.domain.SleepLogSource;
import com.midtone.backend.support.IntegrationTest;
import com.midtone.backend.support.TestUserFixture;
import com.midtone.backend.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ContextRecordPersistenceIntegrationTest extends IntegrationTest {

    @Autowired
    private SleepLogRepository sleepLogRepository;

    @Autowired
    private CaffeineIntakeRepository caffeineIntakeRepository;

    @Autowired
    private TestUserFixture testUserFixture;

    private User user;

    @BeforeEach
    void setUp() {
        caffeineIntakeRepository.deleteAll();
        sleepLogRepository.deleteAll();
        user = testUserFixture.createUserWithSettings("context-record-" + System.nanoTime());
    }

    @Test
    void 수면과_카페인_기록을_저장한다() {
        SleepLog sleep = sleepLogRepository.save(new SleepLog(
                user.getId(),
                LocalDateTime.parse("2026-08-17T23:30:00"),
                LocalDateTime.parse("2026-08-18T07:10:00"),
                "Asia/Seoul",
                SleepLogSource.MANUAL));
        CaffeineIntake intake = caffeineIntakeRepository.save(new CaffeineIntake(
                user.getId(),
                LocalDateTime.parse("2026-08-18T09:00:00"),
                "Asia/Seoul",
                120,
                new BigDecimal("1.00"),
                "COFFEE"));

        assertNotNull(sleep.getId());
        assertEquals(LocalDateTime.parse("2026-08-18T07:10:00"), sleep.getWokeAt());
        assertNotNull(intake.getId());
        assertEquals(new BigDecimal("1.00"), intake.getServings());
    }
}
