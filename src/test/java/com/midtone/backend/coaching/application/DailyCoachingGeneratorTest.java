package com.midtone.backend.coaching.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.midtone.backend.caffeine.application.CaffeineStatusCalculator;
import com.midtone.backend.caffeine.application.DailyCaffeineStatus;
import com.midtone.backend.global.time.DateTimeDefaults;
import com.midtone.backend.shift.application.schedule.NextShiftFinder;
import com.midtone.backend.shift.application.schedule.TransitionDetector;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
import com.midtone.backend.sleep.application.SleepPatternCalculator;
import com.midtone.backend.user.domain.UserSettingsRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyCoachingGeneratorTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 9);

    @Mock
    private ShiftScheduleRepository shiftScheduleRepository;
    @Mock
    private UserSettingsRepository userSettingsRepository;
    @Mock
    private TransitionDetector transitionDetector;
    @Mock
    private CoachingCardGenerator coachingCardGenerator;
    @Mock
    private SleepPatternCalculator sleepPatternCalculator;
    @Mock
    private CaffeineStatusCalculator caffeineStatusCalculator;

    @Test
    void 그날_근무가_아직_시작_전이면_그것이_다음_근무다() {
        ShiftSchedule evening = shift(DATE, ShiftType.EVENING, LocalTime.of(14, 0), LocalTime.of(22, 0));
        givenSchedule(evening, List.of(evening));

        GeneratedCoaching generated = generateAt(DATE.atTime(10, 0));

        assertEquals(DATE.atTime(14, 0), generated.nextShiftStartAt());
    }

    @Test
    void 그날_근무가_이미_시작했으면_그_다음_근무를_찾는다() {
        ShiftSchedule started = shift(DATE, ShiftType.EVENING, LocalTime.of(14, 0), LocalTime.of(22, 0));
        ShiftSchedule tomorrow = shift(DATE.plusDays(1), ShiftType.DAY, LocalTime.of(7, 0), LocalTime.of(16, 0));
        givenSchedule(started, List.of(started, tomorrow));

        GeneratedCoaching generated = generateAt(DATE.atTime(15, 0));

        assertEquals(DATE.plusDays(1).atTime(7, 0), generated.nextShiftStartAt());
    }

    @Test
    void 시작_시각이_없는_근무는_다음_근무로_보지_않는다() {
        ShiftSchedule off = shift(DATE, ShiftType.OFF, null, null);
        ShiftSchedule later = shift(DATE.plusDays(2), ShiftType.NIGHT, LocalTime.of(22, 0), LocalTime.of(7, 0));
        givenSchedule(off, List.of(off, later));

        GeneratedCoaching generated = generateAt(DATE.atTime(10, 0));

        assertEquals(DATE.plusDays(2).atTime(22, 0), generated.nextShiftStartAt());
    }

    @Test
    void 앞으로_잡힌_근무가_없으면_다음_근무도_없다() {
        ShiftSchedule off = shift(DATE, ShiftType.OFF, null, null);
        givenSchedule(off, List.of(off));

        assertNull(generateAt(DATE.atTime(10, 0)).nextShiftStartAt());
    }

    private void givenSchedule(ShiftSchedule todayShift, List<ShiftSchedule> window) {
        when(shiftScheduleRepository.findByUserIdAndWorkDate(1L, DATE)).thenReturn(Optional.of(todayShift));
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(1L, DATE, DATE.plusDays(14)))
                .thenReturn(window);
        lenient().when(userSettingsRepository.findById(1L)).thenReturn(Optional.empty());
        lenient().when(transitionDetector.detectTransition(anyLong(), any(), any())).thenReturn(Optional.empty());
        lenient().when(sleepPatternCalculator.calculate(anyLong(), any())).thenReturn(null);
        lenient().when(caffeineStatusCalculator.calculate(anyLong(), any()))
                .thenReturn(new DailyCaffeineStatus(DATE, 0, java.math.BigDecimal.ZERO, false));
        lenient().when(coachingCardGenerator.generate(any(), any(), anyInt(), any(), any())).thenReturn(List.of());
    }

    private GeneratedCoaching generateAt(LocalDateTime now) {
        Clock clock = Clock.fixed(
                now.atZone(DateTimeDefaults.DEFAULT_ZONE).toInstant(), DateTimeDefaults.DEFAULT_ZONE);
        DailyCoachingGenerator generator = new DailyCoachingGenerator(
                shiftScheduleRepository, userSettingsRepository, transitionDetector, coachingCardGenerator,
                sleepPatternCalculator, caffeineStatusCalculator,
                new NextShiftFinder(shiftScheduleRepository, clock));
        return generator.generate(1L, DATE).orElseThrow();
    }

    private static ShiftSchedule shift(LocalDate date, ShiftType type, LocalTime start, LocalTime end) {
        return new ShiftSchedule(1L, date, type, new ShiftTime(start, end));
    }
}
