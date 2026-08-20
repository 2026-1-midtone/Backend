package com.midtone.backend.home.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.midtone.backend.shift.application.schedule.CompletenessResponse;
import com.midtone.backend.shift.application.schedule.ShiftCompletenessCalculator;
import com.midtone.backend.shift.application.schedule.TransitionDetector;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

    @InjectMocks
    private HomeScheduleSectionBuilder homeScheduleSectionBuilder;

    @Test
    void 오늘_근무와_전환일_여부를_함께_반환한다() {
        LocalDate today = LocalDate.of(2026, 8, 9);
        ShiftSchedule todayShift = new ShiftSchedule(
                1L, today, ShiftType.DAY, new ShiftTime(LocalTime.of(7, 0), LocalTime.of(16, 0)));
        when(shiftScheduleRepository.findByUserIdAndWorkDate(1L, today)).thenReturn(Optional.of(todayShift));
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, today.plusDays(1), today.plusDays(14)))
                .thenReturn(List.of());
        when(transitionDetector.detectTransition(1L, today, ShiftType.DAY))
                .thenReturn(Optional.of(new TransitionDetector.TransitionInfo(ShiftType.NIGHT, ShiftType.DAY)));
        stubSufficientSchedule();

        HomeScheduleSectionBuilder.ScheduleSection section = homeScheduleSectionBuilder.build(1L, today);

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
                        1L, today.plusDays(1), today.plusDays(14)))
                .thenReturn(List.of(off, nextWorking));
        stubSufficientSchedule();

        HomeScheduleSectionBuilder.ScheduleSection section = homeScheduleSectionBuilder.build(1L, today);

        assertEquals("2026-08-11", section.nextShift().workDate());
        assertEquals("NIGHT", section.nextShift().shiftType());
        assertTrue(section.nextShift().startsInMinutes() >= 0);
    }

    @Test
    void 확정된_일정이_하나도_없으면_NO_SCHEDULE_경고를_반환한다() {
        LocalDate today = LocalDate.of(2026, 8, 9);
        when(shiftScheduleRepository.findByUserIdAndWorkDate(1L, today)).thenReturn(Optional.empty());
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, today.plusDays(1), today.plusDays(14)))
                .thenReturn(List.of());
        when(shiftCompletenessCalculator.calculate(4)).thenReturn(new CompletenessResponse(28, 0, 28, List.of()));

        HomeScheduleSectionBuilder.ScheduleSection section = homeScheduleSectionBuilder.build(1L, today);

        assertEquals("NO_SCHEDULE", section.scheduleAlert().type());
        assertEquals(28, section.scheduleAlert().remainingDays());
    }

    @Test
    void 일부만_확정되어_있으면_INSUFFICIENT_SCHEDULE_경고를_반환한다() {
        LocalDate today = LocalDate.of(2026, 8, 9);
        when(shiftScheduleRepository.findByUserIdAndWorkDate(1L, today)).thenReturn(Optional.empty());
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, today.plusDays(1), today.plusDays(14)))
                .thenReturn(List.of());
        when(shiftCompletenessCalculator.calculate(4)).thenReturn(new CompletenessResponse(28, 24, 4, List.of()));

        HomeScheduleSectionBuilder.ScheduleSection section = homeScheduleSectionBuilder.build(1L, today);

        assertEquals("INSUFFICIENT_SCHEDULE", section.scheduleAlert().type());
        assertEquals(4, section.scheduleAlert().remainingDays());
    }

    @Test
    void 최소_일정이_충분하면_경고가_없다() {
        LocalDate today = LocalDate.of(2026, 8, 9);
        when(shiftScheduleRepository.findByUserIdAndWorkDate(1L, today)).thenReturn(Optional.empty());
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, today.plusDays(1), today.plusDays(14)))
                .thenReturn(List.of());
        stubSufficientSchedule();

        HomeScheduleSectionBuilder.ScheduleSection section = homeScheduleSectionBuilder.build(1L, today);

        assertNull(section.scheduleAlert());
    }

    private void stubSufficientSchedule() {
        lenient().when(shiftCompletenessCalculator.calculate(4)).thenReturn(new CompletenessResponse(28, 28, 0, List.of()));
    }
}
