package com.midtone.backend.home.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import com.midtone.backend.global.user.CurrentUserIdProvider;
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
    private HomeActivitySectionBuilder homeActivitySectionBuilder;
    @Mock
    private HomeCoachingSectionBuilder homeCoachingSectionBuilder;

    @InjectMocks
    private HomeService homeService;

    @Test
    void 세_섹션을_조합해서_대시보드를_반환한다() {
        HomeScheduleSectionBuilder.ScheduleSection scheduleSection =
                new HomeScheduleSectionBuilder.ScheduleSection(null, null, false, null);
        HomeActivitySectionBuilder.ActivitySection activitySection = new HomeActivitySectionBuilder.ActivitySection(
                new HomeDashboardResponse.RoutineProgress(0, 0, 0.0), new HomeDashboardResponse.Streak(0), null);
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(homeScheduleSectionBuilder.build(eq(1L), any(LocalDate.class))).thenReturn(scheduleSection);
        when(homeActivitySectionBuilder.build(any(LocalDate.class))).thenReturn(activitySection);
        when(homeCoachingSectionBuilder.build(any(LocalDate.class))).thenReturn(List.of());

        HomeDashboardResponse response = homeService.getDashboard();

        assertEquals(LocalDate.now().toString(), response.date());
        assertEquals(0, response.streak().currentStreak());
        assertEquals(List.of(), response.topCoachingCards());
    }
}
