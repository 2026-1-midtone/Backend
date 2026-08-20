package com.midtone.backend.home.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.midtone.backend.global.time.DateTimeDefaults;
import com.midtone.backend.shift.application.schedule.CompletenessResponse;
import com.midtone.backend.shift.application.schedule.ShiftCompletenessCalculator;
import com.midtone.backend.shift.application.schedule.TransitionDetector;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
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
class HomeScheduleSectionBuilderTest {

    @Mock
    private ShiftScheduleRepository shiftScheduleRepository;
    @Mock
    private TransitionDetector transitionDetector;
    @Mock
    private ShiftCompletenessCalculator shiftCompletenessCalculator;

    @Test
    void 오늘_근무와_전환일_여부를_함께_반환한다() {
        LocalDate today = LocalDate.of(2026, 8, 9);
        ShiftSchedule todayShift = new ShiftSchedule(
                1L, today, ShiftType.DAY, new ShiftTime(LocalTime.of(7, 0), LocalTime.of(16, 0)));
        when(shiftScheduleRepository.findByUserIdAndWorkDate(1L, today)).thenReturn(Optional.of(todayShift));
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, today, today.plusDays(14)))
                .thenReturn(List.of());
        when(transitionDetector.detectTransition(1L, today, ShiftType.DAY))
                .thenReturn(Optional.of(new TransitionDetector.TransitionInfo(ShiftType.NIGHT, ShiftType.DAY)));
        stubSufficientSchedule();

        HomeScheduleSectionBuilder.ScheduleSection section = builderAt(today.atTime(9, 0)).build(1L, today);

        assertEquals("DAY", section.todayShift().shiftType());
        assertEquals("07:00", section.todayShift().startTime());
        assertTrue(section.transitionDay());
    }

    @Test
    void 다음_근무는_OFF를_건너뛰고_가장_가까운_근무를_찾는다() {
        LocalDate today = LocalDate.of(2026, 8, 9);
        ShiftSchedule off = new ShiftSchedule(1L, today.plusDays(1), ShiftType.OFF, new ShiftTime(null, null));
        ShiftSchedule nextWorking = new ShiftSchedule(
                1L, today.plusDays(2), ShiftType.NIGHT, new ShiftTime(LocalTime.of(22, 0), LocalTime.of(7, 0)));
        when(shiftScheduleRepository.findByUserIdAndWorkDate(1L, today)).thenReturn(Optional.empty());
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, today, today.plusDays(14)))
                .thenReturn(List.of(off, nextWorking));
        stubSufficientSchedule();

        HomeScheduleSectionBuilder.ScheduleSection section = builderAt(today.atTime(9, 0)).build(1L, today);

        assertEquals("2026-08-11", section.nextShift().workDate());
        assertEquals("NIGHT", section.nextShift().shiftType());
        assertTrue(section.nextShift().startsInMinutes() >= 0);
    }

    @Test
    void 확정된_일정이_하나도_없으면_NO_SCHEDULE_경고를_반환한다() {
        LocalDate today = LocalDate.of(2026, 8, 9);
        when(shiftScheduleRepository.findByUserIdAndWorkDate(1L, today)).thenReturn(Optional.empty());
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, today, today.plusDays(14)))
                .thenReturn(List.of());
        when(shiftCompletenessCalculator.calculate(4)).thenReturn(new CompletenessResponse(28, 0, 28, List.of()));

        HomeScheduleSectionBuilder.ScheduleSection section = builderAt(today.atTime(9, 0)).build(1L, today);

        assertEquals("NO_SCHEDULE", section.scheduleAlert().type());
        assertEquals(28, section.scheduleAlert().remainingDays());
    }

    @Test
    void 일부만_확정되어_있으면_INSUFFICIENT_SCHEDULE_경고를_반환한다() {
        LocalDate today = LocalDate.of(2026, 8, 9);
        when(shiftScheduleRepository.findByUserIdAndWorkDate(1L, today)).thenReturn(Optional.empty());
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, today, today.plusDays(14)))
                .thenReturn(List.of());
        when(shiftCompletenessCalculator.calculate(4)).thenReturn(new CompletenessResponse(28, 24, 4, List.of()));

        HomeScheduleSectionBuilder.ScheduleSection section = builderAt(today.atTime(9, 0)).build(1L, today);

        assertEquals("INSUFFICIENT_SCHEDULE", section.scheduleAlert().type());
        assertEquals(4, section.scheduleAlert().remainingDays());
    }

    @Test
    void 최소_일정이_충분하면_경고가_없다() {
        LocalDate today = LocalDate.of(2026, 8, 9);
        when(shiftScheduleRepository.findByUserIdAndWorkDate(1L, today)).thenReturn(Optional.empty());
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, today, today.plusDays(14)))
                .thenReturn(List.of());
        stubSufficientSchedule();

        HomeScheduleSectionBuilder.ScheduleSection section = builderAt(today.atTime(9, 0)).build(1L, today);

        assertNull(section.scheduleAlert());
    }

    @Test
    void 오늘_근무가_아직_시작_전이면_그것이_다음_근무다() {
        LocalDate today = LocalDate.of(2026, 8, 9);
        ShiftSchedule evening = new ShiftSchedule(
                1L, today, ShiftType.EVENING, new ShiftTime(LocalTime.of(14, 0), LocalTime.of(22, 0)));
        when(shiftScheduleRepository.findByUserIdAndWorkDate(1L, today)).thenReturn(Optional.of(evening));
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, today, today.plusDays(14)))
                .thenReturn(List.of(evening));
        when(transitionDetector.detectTransition(1L, today, ShiftType.EVENING)).thenReturn(Optional.empty());
        stubSufficientSchedule();

        HomeScheduleSectionBuilder.ScheduleSection section =
                builderAt(today.atTime(10, 0)).build(1L, today);

        assertEquals("2026-08-09", section.nextShift().workDate());
        assertEquals(240, section.nextShift().startsInMinutes());
    }

    @Test
    void 오늘_근무가_이미_시작했으면_그_다음_근무를_찾는다() {
        LocalDate today = LocalDate.of(2026, 8, 9);
        ShiftSchedule started = new ShiftSchedule(
                1L, today, ShiftType.EVENING, new ShiftTime(LocalTime.of(14, 0), LocalTime.of(22, 0)));
        ShiftSchedule tomorrow = new ShiftSchedule(
                1L, today.plusDays(1), ShiftType.DAY, new ShiftTime(LocalTime.of(7, 0), LocalTime.of(16, 0)));
        when(shiftScheduleRepository.findByUserIdAndWorkDate(1L, today)).thenReturn(Optional.of(started));
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, today, today.plusDays(14)))
                .thenReturn(List.of(started, tomorrow));
        when(transitionDetector.detectTransition(1L, today, ShiftType.EVENING)).thenReturn(Optional.empty());
        stubSufficientSchedule();

        HomeScheduleSectionBuilder.ScheduleSection section =
                builderAt(today.atTime(15, 0)).build(1L, today);

        assertEquals("2026-08-10", section.nextShift().workDate());
    }

    private HomeScheduleSectionBuilder builderAt(LocalDateTime now) {
        return new HomeScheduleSectionBuilder(shiftScheduleRepository, transitionDetector, shiftCompletenessCalculator,
                Clock.fixed(now.atZone(DateTimeDefaults.DEFAULT_ZONE).toInstant(), DateTimeDefaults.DEFAULT_ZONE));
    }

    private void stubSufficientSchedule() {
        lenient().when(shiftCompletenessCalculator.calculate(4)).thenReturn(new CompletenessResponse(28, 28, 0, List.of()));
    }
}
