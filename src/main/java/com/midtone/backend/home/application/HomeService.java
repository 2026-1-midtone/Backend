package com.midtone.backend.home.application;

import com.midtone.backend.global.time.DateTimeDefaults;
import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.nap.application.NapService;
import com.midtone.backend.routine.application.RoutineService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HomeService {

    private static final String STREAK_REPORT_PERIOD = "7d";

    private final CurrentUserIdProvider currentUserIdProvider;
    private final HomeScheduleSectionBuilder homeScheduleSectionBuilder;
    private final HomeCoachingSectionBuilder homeCoachingSectionBuilder;
    private final RoutineService routineService;
    private final NapService napService;

    public HomeService(
            CurrentUserIdProvider currentUserIdProvider,
            HomeScheduleSectionBuilder homeScheduleSectionBuilder,
            HomeCoachingSectionBuilder homeCoachingSectionBuilder,
            RoutineService routineService,
            NapService napService) {
        this.currentUserIdProvider = currentUserIdProvider;
        this.homeScheduleSectionBuilder = homeScheduleSectionBuilder;
        this.homeCoachingSectionBuilder = homeCoachingSectionBuilder;
        this.routineService = routineService;
        this.napService = napService;
    }

    public HomeDashboardResponse getDashboard() {
        long userId = currentUserIdProvider.getCurrentUserId();
        LocalDate today = LocalDate.now(DateTimeDefaults.DEFAULT_ZONE);
        HomeScheduleSectionBuilder.ScheduleSection scheduleSection = homeScheduleSectionBuilder.build(userId, today);
        List<HomeDashboardResponse.TopCoachingCard> topCoachingCards = homeCoachingSectionBuilder.build(today);
        RoutineService.DailySummary summary = routineService.getSummary(today);
        RoutineService.RoutineReport report = routineService.getReport(STREAK_REPORT_PERIOD);
        return HomeDashboardResponse.of(
                today,
                scheduleSection,
                toRoutineProgress(summary),
                toStreak(report),
                napService.getActiveNap(),
                topCoachingCards);
    }

    private HomeDashboardResponse.RoutineProgress toRoutineProgress(RoutineService.DailySummary summary) {
        return new HomeDashboardResponse.RoutineProgress(
                summary.totalCount(), summary.doneCount(), summary.completionRate());
    }

    private HomeDashboardResponse.Streak toStreak(RoutineService.RoutineReport report) {
        return new HomeDashboardResponse.Streak(report.streak().currentStreak());
    }
}
