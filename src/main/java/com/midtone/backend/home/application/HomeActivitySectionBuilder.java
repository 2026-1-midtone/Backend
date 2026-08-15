package com.midtone.backend.home.application;

import com.midtone.backend.nap.application.NapService;
import com.midtone.backend.routine.application.RoutineService;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class HomeActivitySectionBuilder {

    private static final String STREAK_REPORT_PERIOD = "7d";

    private final RoutineService routineService;
    private final NapService napService;

    public HomeActivitySectionBuilder(RoutineService routineService, NapService napService) {
        this.routineService = routineService;
        this.napService = napService;
    }

    public ActivitySection build(LocalDate date) {
        RoutineService.DailySummary summary = routineService.getSummary(date);
        RoutineService.RoutineReport report = routineService.getReport(STREAK_REPORT_PERIOD);
        return new ActivitySection(toRoutineProgress(summary), toStreak(report), napService.getActiveNap());
    }

    private HomeDashboardResponse.RoutineProgress toRoutineProgress(RoutineService.DailySummary summary) {
        return new HomeDashboardResponse.RoutineProgress(
                summary.totalCount(), summary.doneCount(), summary.completionRate());
    }

    private HomeDashboardResponse.Streak toStreak(RoutineService.RoutineReport report) {
        return new HomeDashboardResponse.Streak(report.streak().currentStreak());
    }

    public record ActivitySection(
            HomeDashboardResponse.RoutineProgress routineProgress,
            HomeDashboardResponse.Streak streak,
            NapService.ActiveNap activeNap) {
    }
}
