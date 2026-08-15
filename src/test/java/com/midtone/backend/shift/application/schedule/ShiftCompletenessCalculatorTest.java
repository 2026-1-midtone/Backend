package com.midtone.backend.shift.application.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShiftCompletenessCalculatorTest {

    @Mock
    private ShiftScheduleRepository shiftScheduleRepository;
    @Mock
    private CurrentUserIdProvider currentUserIdProvider;

    @InjectMocks
    private ShiftCompletenessCalculator shiftCompletenessCalculator;

    @Test
    void 최소_4주_충족_현황을_조회한다() {
        LocalDate today = LocalDate.now();
        ShiftSchedule confirmedShift = new ShiftSchedule(1L, today, ShiftType.DAY, new ShiftTime(null, null));
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                        1L, today, today.plusDays(27)))
                .thenReturn(List.of(confirmedShift));

        CompletenessResponse response = shiftCompletenessCalculator.calculate(4);

        assertEquals(28, response.requiredDays());
        assertEquals(1, response.confirmedDays());
        assertEquals(27, response.remainingDays());
        assertEquals(27, response.missingDates().size());
    }

    @Test
    void 특정_기간의_충족_현황을_조회한다() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 28);
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(1L, from, to))
                .thenReturn(List.of());

        CompletenessResponse response = shiftCompletenessCalculator.calculate(from, to);

        assertEquals(28, response.requiredDays());
        assertEquals(0, response.confirmedDays());
        assertEquals(28, response.remainingDays());
    }
}
