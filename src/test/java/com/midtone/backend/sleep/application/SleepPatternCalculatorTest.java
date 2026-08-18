package com.midtone.backend.sleep.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;

import com.midtone.backend.sleep.domain.SleepLog;
import com.midtone.backend.sleep.domain.SleepLogRepository;
import com.midtone.backend.sleep.domain.SleepLogSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SleepPatternCalculatorTest {

    @Mock
    private SleepLogRepository sleepLogRepository;

    private SleepPatternCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new SleepPatternCalculator(sleepLogRepository);
    }

    @Test
    void 자정_전후_취침시각을_밤_시각으로_평균한다() {
        LocalDate today = LocalDate.parse("2026-08-18");
        given(sleepLogRepository.findByUserIdAndWokeAtGreaterThanEqualAndWokeAtLessThanOrderByWokeAtAsc(
                1L, today.minusDays(13).atStartOfDay(), today.plusDays(1).atStartOfDay()))
                .willReturn(List.of(
                        sleep("2026-08-16T23:00:00", "2026-08-17T07:00:00"),
                        sleep("2026-08-18T01:00:00", "2026-08-18T09:00:00")));

        SleepPattern result = calculator.calculate(1L, today);

        assertEquals(LocalTime.MIDNIGHT, result.habitualBedtime());
        assertEquals(LocalTime.of(8, 0), result.habitualWakeTime());
        assertEquals(LocalTime.of(4, 0), result.habitualMidSleep());
        assertEquals(LocalTime.of(6, 0), result.estimatedCbtMin());
        assertEquals(2, result.sampleCount());
        assertEquals(today.minusDays(13), result.windowFrom());
        assertEquals(today, result.windowTo());
    }

    @Test
    void 기록이_없으면_시각을_추정하지_않는다() {
        LocalDate today = LocalDate.parse("2026-08-18");
        given(sleepLogRepository.findByUserIdAndWokeAtGreaterThanEqualAndWokeAtLessThanOrderByWokeAtAsc(
                1L, today.minusDays(13).atStartOfDay(), today.plusDays(1).atStartOfDay()))
                .willReturn(List.of());

        SleepPattern result = calculator.calculate(1L, today);

        assertNull(result.habitualBedtime());
        assertNull(result.habitualWakeTime());
        assertNull(result.habitualMidSleep());
        assertNull(result.estimatedCbtMin());
        assertEquals(0, result.sampleCount());
    }

    private SleepLog sleep(String sleptAt, String wokeAt) {
        return new SleepLog(
                1L,
                LocalDateTime.parse(sleptAt),
                LocalDateTime.parse(wokeAt),
                "Asia/Seoul",
                SleepLogSource.MANUAL);
    }
}
