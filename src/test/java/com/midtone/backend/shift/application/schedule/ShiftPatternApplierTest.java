package com.midtone.backend.shift.application.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.shift.application.ShiftException;
import com.midtone.backend.shift.application.pattern.SaveShiftPatternRequest;
import com.midtone.backend.shift.application.pattern.ShiftPatternResponse;
import com.midtone.backend.shift.application.pattern.ShiftPatternService;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShiftPatternApplierTest {

    @Mock
    private ShiftScheduleRepository shiftScheduleRepository;
    @Mock
    private CurrentUserIdProvider currentUserIdProvider;
    @Mock
    private ShiftPatternService shiftPatternService;
    @Mock
    private ShiftCompletenessCalculator shiftCompletenessCalculator;
    @Mock
    private ShiftCoachingRegenerationTrigger shiftCoachingRegenerationTrigger;
    @Mock
    private ShiftTimeDefaultService shiftTimeDefaultService;

    @InjectMocks
    private ShiftPatternApplier shiftPatternApplier;

    @BeforeEach
    void setUp() {
        lenient().when(shiftTimeDefaultService.resolve(anyLong(), any()))
                .thenReturn(new ShiftTime(null, null));
    }

    @Test
    void 반복_패턴으로_새_일정을_생성한다() {
        ApplyShiftPatternRequest request = new ApplyShiftPatternRequest(
                "2026-09-01", 4, List.of("DAY", "EVENING", "NIGHT", "OFF"), null, null);
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 28)))
                .thenReturn(List.of());
        when(shiftCompletenessCalculator.calculate(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 28)))
                .thenReturn(new CompletenessResponse(28, 28, 0, List.of()));

        ApplyShiftPatternResponse response = shiftPatternApplier.apply(request);

        assertEquals(28, response.createdCount());
        assertEquals(0, response.updatedCount());
        assertNull(response.patternId());
        assertEquals(28, response.completeness().requiredDays());
    }

    @Test
    void 기존_일정이_있으면_수정하고_생성_수를_구분한다() {
        ApplyShiftPatternRequest request = new ApplyShiftPatternRequest(
                "2026-09-01", 4, List.of("DAY", "EVENING", "NIGHT", "OFF"), null, null);
        ShiftSchedule existing = new ShiftSchedule(
                1L, LocalDate.of(2026, 9, 1), ShiftType.OFF, new ShiftTime(null, null));
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 28)))
                .thenReturn(List.of(existing));
        when(shiftCompletenessCalculator.calculate(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 28)))
                .thenReturn(new CompletenessResponse(28, 1, 27, List.of()));

        ApplyShiftPatternResponse response = shiftPatternApplier.apply(request);

        assertEquals(27, response.createdCount());
        assertEquals(1, response.updatedCount());
        assertEquals(ShiftType.DAY, existing.getShiftType());
    }

    @Test
    void saveAsPattern이_true이면_패턴을_저장한다() {
        ApplyShiftPatternRequest request = new ApplyShiftPatternRequest(
                "2026-09-01", 4, List.of("DAY", "EVENING", "NIGHT", "OFF"), true, "내 패턴");
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 28)))
                .thenReturn(List.of());
        when(shiftCompletenessCalculator.calculate(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 28)))
                .thenReturn(new CompletenessResponse(28, 28, 0, List.of()));
        when(shiftPatternService.saveShiftPattern(any(SaveShiftPatternRequest.class)))
                .thenReturn(new ShiftPatternResponse(10L, "내 패턴", 4, List.of("DAY", "EVENING", "NIGHT", "OFF")));

        ApplyShiftPatternResponse response = shiftPatternApplier.apply(request);

        verify(shiftPatternService).saveShiftPattern(any(SaveShiftPatternRequest.class));
        assertEquals(10L, response.patternId());
    }

    @Test
    void 패턴을_적용하면_해당_기간의_코칭을_재생성한다() {
        ApplyShiftPatternRequest request = new ApplyShiftPatternRequest(
                "2026-09-01", 4, List.of("DAY", "EVENING", "NIGHT", "OFF"), null, null);
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 28)))
                .thenReturn(List.of());
        when(shiftCompletenessCalculator.calculate(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 28)))
                .thenReturn(new CompletenessResponse(28, 28, 0, List.of()));
        when(shiftCoachingRegenerationTrigger.triggerForRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 28)))
                .thenReturn(List.of("2026-09-01", "2026-09-02"));

        ApplyShiftPatternResponse response = shiftPatternApplier.apply(request);

        verify(shiftCoachingRegenerationTrigger)
                .triggerForRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 28));
        assertEquals(List.of("2026-09-01", "2026-09-02"), response.affectedCoachingDates());
    }

    @Test
    void saveAsPattern이_true인데_이름이_없으면_예외를_던진다() {
        ApplyShiftPatternRequest request = new ApplyShiftPatternRequest(
                "2026-09-01", 4, List.of("DAY", "EVENING", "NIGHT", "OFF"), true, null);

        ShiftException exception =
                assertThrows(ShiftException.class, () -> shiftPatternApplier.apply(request));
        assertEquals(ShiftException.ErrorCode.PATTERN_NAME_REQUIRED, exception.getErrorCode());
    }
}
