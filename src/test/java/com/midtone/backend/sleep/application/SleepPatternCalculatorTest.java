package com.midtone.backend.sleep.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;

import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
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
    @Mock
    private ShiftScheduleRepository shiftScheduleRepository;

    private SleepPatternCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new SleepPatternCalculator(sleepLogRepository, shiftScheduleRepository);
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

    @Test
    void 오늘과_같은_근무_유형_뒤_수면_기록만_걸러_평균한다() {
        LocalDate today = LocalDate.parse("2026-08-18");
        given(sleepLogRepository.findByUserIdAndWokeAtGreaterThanEqualAndWokeAtLessThanOrderByWokeAtAsc(
                1L, today.minusDays(13).atStartOfDay(), today.plusDays(1).atStartOfDay()))
                .willReturn(List.of(
                        sleep("2026-08-15T07:30:00", "2026-08-15T13:30:00"),
                        sleep("2026-08-16T07:15:00", "2026-08-16T13:15:00"),
                        sleep("2026-08-17T07:45:00", "2026-08-17T13:45:00"),
                        sleep("2026-08-17T23:00:00", "2026-08-18T07:00:00")));
        given(shiftScheduleRepository.findByUserIdAndWorkDate(1L, today))
                .willReturn(java.util.Optional.of(night(today)));
        given(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                1L, today.minusDays(14), today))
                .willReturn(List.of(
                        night(LocalDate.parse("2026-08-14")),
                        night(LocalDate.parse("2026-08-15")),
                        night(LocalDate.parse("2026-08-16")),
                        day(LocalDate.parse("2026-08-17")),
                        night(today)));

        SleepPattern result = calculator.calculate(1L, today);

        // 나이트 근무(08-14~16) 뒤 수면 3건만 반영되고, 데이 근무(08-17) 뒤 수면은 제외된다.
        assertEquals(LocalTime.of(7, 30), result.habitualBedtime());
        assertEquals(LocalTime.of(13, 30), result.habitualWakeTime());
        assertEquals(LocalTime.of(10, 30), result.habitualMidSleep());
        assertEquals(LocalTime.of(11, 30), result.estimatedCbtMin());
        assertEquals(3, result.sampleCount());
    }

    @Test
    void 같은_근무_유형_표본이_최소치보다_적으면_평균하지_않는다() {
        LocalDate today = LocalDate.parse("2026-08-18");
        given(sleepLogRepository.findByUserIdAndWokeAtGreaterThanEqualAndWokeAtLessThanOrderByWokeAtAsc(
                1L, today.minusDays(13).atStartOfDay(), today.plusDays(1).atStartOfDay()))
                .willReturn(List.of(
                        sleep("2026-08-18T07:30:00", "2026-08-18T13:30:00"),
                        sleep("2026-08-16T23:00:00", "2026-08-17T07:00:00")));
        given(shiftScheduleRepository.findByUserIdAndWorkDate(1L, today))
                .willReturn(java.util.Optional.of(night(today)));
        given(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                1L, today.minusDays(14), today))
                .willReturn(List.of(
                        day(LocalDate.parse("2026-08-16")),
                        night(LocalDate.parse("2026-08-17")),
                        night(today)));

        SleepPattern result = calculator.calculate(1L, today);

        // 나이트 근무 뒤 수면이 1건뿐이라 최소 표본(3건)에 못 미쳐 평균을 내지 않는다.
        assertNull(result.habitualBedtime());
        assertEquals(0, result.sampleCount());
    }

    private ShiftSchedule night(LocalDate workDate) {
        return new ShiftSchedule(1L, workDate, ShiftType.NIGHT, new ShiftTime(LocalTime.of(22, 0), LocalTime.of(7, 0)));
    }

    private ShiftSchedule day(LocalDate workDate) {
        return new ShiftSchedule(1L, workDate, ShiftType.DAY, new ShiftTime(LocalTime.of(9, 0), LocalTime.of(18, 0)));
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
