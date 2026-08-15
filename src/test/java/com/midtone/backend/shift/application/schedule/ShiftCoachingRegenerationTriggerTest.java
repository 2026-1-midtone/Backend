package com.midtone.backend.shift.application.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.midtone.backend.coaching.application.CoachingService;
import com.midtone.backend.coaching.application.RegenerateCoachingRequest;
import com.midtone.backend.coaching.application.RegenerateCoachingResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShiftCoachingRegenerationTriggerTest {

    @Mock
    private CoachingService coachingService;

    @InjectMocks
    private ShiftCoachingRegenerationTrigger shiftCoachingRegenerationTrigger;

    @Test
    void 단일_수정은_변경일과_다음날_구간으로_재생성을_요청한다() {
        LocalDate workDate = LocalDate.of(2026, 8, 5);
        RegenerateCoachingResponse response =
                new RegenerateCoachingResponse(List.of("2026-08-05", "2026-08-06"), 2);
        when(coachingService.regenerateCoaching(new RegenerateCoachingRequest("2026-08-05", "2026-08-06")))
                .thenReturn(response);

        List<String> affectedDates = shiftCoachingRegenerationTrigger.triggerForSingleUpdate(workDate);

        assertEquals(List.of("2026-08-05", "2026-08-06"), affectedDates);
    }

    @Test
    void 일괄_변경은_요청받은_범위_그대로_재생성을_요청한다() {
        LocalDate from = LocalDate.of(2026, 8, 10);
        LocalDate to = LocalDate.of(2026, 8, 14);
        RegenerateCoachingResponse response = new RegenerateCoachingResponse(
                List.of("2026-08-10", "2026-08-11", "2026-08-12", "2026-08-13", "2026-08-14"), 5);
        when(coachingService.regenerateCoaching(new RegenerateCoachingRequest("2026-08-10", "2026-08-14")))
                .thenReturn(response);

        List<String> affectedDates = shiftCoachingRegenerationTrigger.triggerForRange(from, to);

        assertEquals(5, affectedDates.size());
    }
}
