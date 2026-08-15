package com.midtone.backend.home.application;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HomeService {

    private final CurrentUserIdProvider currentUserIdProvider;
    private final HomeScheduleSectionBuilder homeScheduleSectionBuilder;
    private final HomeActivitySectionBuilder homeActivitySectionBuilder;
    private final HomeCoachingSectionBuilder homeCoachingSectionBuilder;

    public HomeService(
            CurrentUserIdProvider currentUserIdProvider,
            HomeScheduleSectionBuilder homeScheduleSectionBuilder,
            HomeActivitySectionBuilder homeActivitySectionBuilder,
            HomeCoachingSectionBuilder homeCoachingSectionBuilder) {
        this.currentUserIdProvider = currentUserIdProvider;
        this.homeScheduleSectionBuilder = homeScheduleSectionBuilder;
        this.homeActivitySectionBuilder = homeActivitySectionBuilder;
        this.homeCoachingSectionBuilder = homeCoachingSectionBuilder;
    }

    public HomeDashboardResponse getDashboard() {
        long userId = currentUserIdProvider.getCurrentUserId();
        LocalDate today = LocalDate.now();
        HomeScheduleSectionBuilder.ScheduleSection scheduleSection = homeScheduleSectionBuilder.build(userId, today);
        HomeActivitySectionBuilder.ActivitySection activitySection = homeActivitySectionBuilder.build(today);
        List<HomeDashboardResponse.TopCoachingCard> topCoachingCards = homeCoachingSectionBuilder.build(today);
        return HomeDashboardResponse.of(today, scheduleSection, activitySection, topCoachingCards);
    }
}
