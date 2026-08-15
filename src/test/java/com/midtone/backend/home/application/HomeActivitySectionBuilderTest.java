package com.midtone.backend.home.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.midtone.backend.nap.application.NapService;
import com.midtone.backend.routine.application.RoutineService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HomeActivitySectionBuilderTest {

    @Mock
    private RoutineService routineService;
    @Mock
    private NapService napService;

    @InjectMocks
    private HomeActivitySectionBuilder homeActivitySectionBuilder;

    @Test
    void 루틴_진행률과_스트릭과_활성_낮잠을_함께_반환한다() {
        LocalDate date = LocalDate.of(2026, 8, 9);
        RoutineService.DailySummary summary =
                new RoutineService.DailySummary(date, 6, 4, 1, 0.67, List.of("파워냅"), List.of("빛 노출"));
        RoutineService.RoutineReport report = new RoutineService.RoutineReport(
                "7d", date.minusDays(6), date, 0.71, new RoutineService.Streak(12, 21, date));
        when(routineService.getSummary(date)).thenReturn(summary);
        when(routineService.getReport("7d")).thenReturn(report);
        when(napService.getActiveNap()).thenReturn(null);

        HomeActivitySectionBuilder.ActivitySection section = homeActivitySectionBuilder.build(date);

        assertEquals(6, section.routineProgress().total());
        assertEquals(4, section.routineProgress().done());
        assertEquals(0.67, section.routineProgress().completionRate());
        assertEquals(12, section.streak().currentStreak());
        assertNull(section.activeNap());
    }
}
