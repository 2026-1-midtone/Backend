package com.midtone.backend.shift.application.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransitionDetectorTest {

    @Mock
    private ShiftScheduleRepository shiftScheduleRepository;

    @InjectMocks
    private TransitionDetector transitionDetector;

    @Test
    void 이전_근무_유형과_다르면_전환일로_판단한다() {
        ShiftSchedule previousNight = new ShiftSchedule(
                1L, LocalDate.of(2026, 8, 8), ShiftType.NIGHT, new ShiftTime(null, null));
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, LocalDate.of(2026, 7, 26), LocalDate.of(2026, 8, 8)))
                .thenReturn(List.of(previousNight));

        Optional<TransitionDetector.TransitionInfo> result =
                transitionDetector.detectTransition(1L, LocalDate.of(2026, 8, 9), ShiftType.DAY);

        assertTrue(result.isPresent());
        assertEquals(ShiftType.NIGHT, result.get().fromShiftType());
        assertEquals(ShiftType.DAY, result.get().toShiftType());
    }

    @Test
    void OFF를_건너뛰고_직전_근무_유형과_비교한다() {
        ShiftSchedule night = new ShiftSchedule(1L, LocalDate.of(2026, 8, 7), ShiftType.NIGHT, new ShiftTime(null, null));
        ShiftSchedule off = new ShiftSchedule(1L, LocalDate.of(2026, 8, 8), ShiftType.OFF, new ShiftTime(null, null));
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, LocalDate.of(2026, 7, 26), LocalDate.of(2026, 8, 8)))
                .thenReturn(List.of(night, off));

        Optional<TransitionDetector.TransitionInfo> result =
                transitionDetector.detectTransition(1L, LocalDate.of(2026, 8, 9), ShiftType.DAY);

        assertTrue(result.isPresent());
        assertEquals(ShiftType.NIGHT, result.get().fromShiftType());
    }

    @Test
    void 같은_유형이_이어지면_전환일이_아니다() {
        ShiftSchedule previousNight = new ShiftSchedule(
                1L, LocalDate.of(2026, 8, 8), ShiftType.NIGHT, new ShiftTime(null, null));
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, LocalDate.of(2026, 7, 26), LocalDate.of(2026, 8, 8)))
                .thenReturn(List.of(previousNight));

        Optional<TransitionDetector.TransitionInfo> result =
                transitionDetector.detectTransition(1L, LocalDate.of(2026, 8, 9), ShiftType.NIGHT);

        assertTrue(result.isEmpty());
    }

    @Test
    void 오늘이_OFF면_전환일이_아니다() {
        Optional<TransitionDetector.TransitionInfo> result =
                transitionDetector.detectTransition(1L, LocalDate.of(2026, 8, 9), ShiftType.OFF);

        assertTrue(result.isEmpty());
    }

    @Test
    void 정렬된_일정_목록에서_전환일_집합을_계산한다() {
        List<ShiftSchedule> shifts = List.of(
                new ShiftSchedule(1L, LocalDate.of(2026, 8, 8), ShiftType.NIGHT, new ShiftTime(null, null)),
                new ShiftSchedule(1L, LocalDate.of(2026, 8, 9), ShiftType.OFF, new ShiftTime(null, null)),
                new ShiftSchedule(1L, LocalDate.of(2026, 8, 10), ShiftType.DAY, new ShiftTime(null, null)),
                new ShiftSchedule(1L, LocalDate.of(2026, 8, 11), ShiftType.DAY, new ShiftTime(null, null)));

        Set<LocalDate> result = TransitionDetector.transitionDaysOf(shifts);

        assertEquals(Set.of(LocalDate.of(2026, 8, 10)), result);
    }

    @Test
    void 이전_근무가_없으면_전환일_집합은_비어있다() {
        List<ShiftSchedule> shifts = List.of(
                new ShiftSchedule(1L, LocalDate.of(2026, 8, 8), ShiftType.NIGHT, new ShiftTime(null, null)));

        Set<LocalDate> result = TransitionDetector.transitionDaysOf(shifts);

        assertTrue(result.isEmpty());
    }
}
