package com.midtone.backend.sleep.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.sleep.domain.SleepLog;
import com.midtone.backend.sleep.domain.SleepLogRepository;
import com.midtone.backend.sleep.domain.SleepLogSource;
import com.midtone.backend.user.domain.User;
import com.midtone.backend.user.domain.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SleepLogServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-18T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private CurrentUserIdProvider currentUserIdProvider;

    @Mock
    private SleepLogRepository sleepLogRepository;

    @Mock
    private UserRepository userRepository;

    private SleepLogService service;

    @BeforeEach
    void setUp() {
        service = new SleepLogService(currentUserIdProvider, sleepLogRepository, userRepository, CLOCK);
    }

    @Test
    void 수면기록을_사용자_타임존으로_저장한다() {
        given(currentUserIdProvider.getCurrentUserId()).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user()));
        given(sleepLogRepository.countByUserIdAndSleptAtLessThanAndWokeAtGreaterThan(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(0L);
        given(sleepLogRepository.save(any(SleepLog.class))).willAnswer(invocation -> {
            SleepLog saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });

        SleepLogResponse result = service.create(new CreateSleepLogRequest(
                OffsetDateTime.parse("2026-08-17T23:30:00+09:00"),
                OffsetDateTime.parse("2026-08-18T07:10:00+09:00"), null));

        assertEquals(10L, result.sleepLogId());
        assertEquals(OffsetDateTime.parse("2026-08-17T23:30:00+09:00"), result.sleptAt());
        assertEquals("MANUAL", result.source());
    }

    @Test
    void 기상시각이_취침시각보다_늦지_않으면_거절한다() {
        given(currentUserIdProvider.getCurrentUserId()).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user()));

        SleepLogException error = assertThrows(SleepLogException.class, () -> service.create(
                new CreateSleepLogRequest(
                        OffsetDateTime.parse("2026-08-18T08:00:00+09:00"),
                        OffsetDateTime.parse("2026-08-18T07:00:00+09:00"), "MANUAL")));

        assertEquals(SleepLogException.ErrorCode.INVALID_INTERVAL, error.getErrorCode());
    }

    @Test
    void 미래_기상시각은_거절한다() {
        given(currentUserIdProvider.getCurrentUserId()).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user()));

        SleepLogException error = assertThrows(SleepLogException.class, () -> service.create(
                new CreateSleepLogRequest(
                        OffsetDateTime.parse("2026-08-19T08:00:00+09:00"),
                        OffsetDateTime.parse("2026-08-19T16:00:00+09:00"), "MANUAL")));

        assertEquals(SleepLogException.ErrorCode.FUTURE_RECORD, error.getErrorCode());
    }

    @Test
    void 기존_수면과_겹치면_거절한다() {
        given(currentUserIdProvider.getCurrentUserId()).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user()));
        given(sleepLogRepository.countByUserIdAndSleptAtLessThanAndWokeAtGreaterThan(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(1L);

        SleepLogException error = assertThrows(SleepLogException.class, () -> service.create(
                new CreateSleepLogRequest(
                        OffsetDateTime.parse("2026-08-17T23:30:00+09:00"),
                        OffsetDateTime.parse("2026-08-18T07:10:00+09:00"), "MANUAL")));

        assertEquals(SleepLogException.ErrorCode.OVERLAPPING_LOG, error.getErrorCode());
    }

    @Test
    void 타인의_수면기록은_수정할_수_없다() {
        given(currentUserIdProvider.getCurrentUserId()).willReturn(1L);
        given(sleepLogRepository.findById(10L)).willReturn(Optional.of(sleepLog(2L)));

        SleepLogException error = assertThrows(SleepLogException.class, () -> service.update(
                10L, new UpdateSleepLogRequest(null, null, "MANUAL")));

        assertEquals(SleepLogException.ErrorCode.ACCESS_DENIED, error.getErrorCode());
    }

    @Test
    void 기간의_수면기록을_오래된순으로_반환한다() {
        given(currentUserIdProvider.getCurrentUserId()).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user()));
        given(sleepLogRepository.findByUserIdAndWokeAtBetweenOrderByWokeAtAsc(
                1L, LocalDateTime.parse("2026-08-17T00:00:00"), LocalDateTime.parse("2026-08-19T00:00:00")))
                .willReturn(List.of(sleepLog(1L)));

        SleepLogListResponse result = service.getLogs(
                LocalDate.parse("2026-08-17"), LocalDate.parse("2026-08-18"));

        assertEquals(1, result.logs().size());
        assertEquals("Asia/Seoul", result.logs().get(0).recordedTimezone());
    }

    private User user() {
        return new User("google-1", "user@example.com", "사용자", null);
    }

    private SleepLog sleepLog(long userId) {
        SleepLog log = new SleepLog(
                userId,
                LocalDateTime.parse("2026-08-17T23:30:00"),
                LocalDateTime.parse("2026-08-18T07:10:00"),
                "Asia/Seoul",
                SleepLogSource.MANUAL);
        ReflectionTestUtils.setField(log, "id", 10L);
        return log;
    }
}
