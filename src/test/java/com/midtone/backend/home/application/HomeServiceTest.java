package com.midtone.backend.home.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import com.midtone.backend.global.user.CurrentUserIdProvider;
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
class HomeServiceTest {

    @Mock
    private CurrentUserIdProvider currentUserIdProvider;
    @Mock
    private HomeScheduleSectionBuilder homeScheduleSectionBuilder;
    @Mock
    private HomeCoachingSectionBuilder homeCoachingSectionBuilder;
    @Mock
    private RoutineService routineService;
    @Mock
    private NapService napService;

    @InjectMocks
    private HomeService homeService;

    @Test
    void 근무_루틴_코칭_낮잠_섹션을_조합해서_대시보드를_반환한다() {
        HomeScheduleSectionBuilder.ScheduleSection scheduleSection =
                new HomeScheduleSectionBuilder.ScheduleSection(null, null, false, null);
        RoutineService.DailySummary summary =
                new RoutineService.DailySummary(LocalDate.now(), 6, 4, 1, 0.67, List.of(), List.of());
        RoutineService.RoutineReport report = new RoutineService.RoutineReport(
                "7d", LocalDate.now().minusDays(6), LocalDate.now(), 0.71, new RoutineService.Streak(12, 21, null));
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(homeScheduleSectionBuilder.build(eq(1L), any(LocalDate.class))).thenReturn(scheduleSection);
        when(homeCoachingSectionBuilder.build(any(LocalDate.class))).thenReturn(List.of());
        when(routineService.getSummary(any(LocalDate.class))).thenReturn(summary);
        when(routineService.getReport("7d")).thenReturn(report);
        when(napService.getActiveNap()).thenReturn(null);

        HomeDashboardResponse response = homeService.getDashboard();

        assertEquals(LocalDate.now().toString(), response.date());
        assertEquals(6, response.routineProgress().total());
        assertEquals(4, response.routineProgress().done());
        assertEquals(0.67, response.routineProgress().completionRate());
        assertEquals(12, response.streak().currentStreak());
        assertEquals(List.of(), response.topCoachingCards());
    }
}
