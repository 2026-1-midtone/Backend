package com.midtone.backend.home.application;

import com.midtone.backend.nap.application.NapService;
import java.time.LocalDate;
import java.util.List;

public record HomeDashboardResponse(
        String date,
        TodayShift todayShift,
        NextShift nextShift,
        boolean isTransitionDay,
        RoutineProgress routineProgress,
        List<TopCoachingCard> topCoachingCards,
        Streak streak,
        NapService.ActiveNap activeNap,
        ScheduleAlert scheduleAlert) {

    public static HomeDashboardResponse of(
            LocalDate date,
            HomeScheduleSectionBuilder.ScheduleSection scheduleSection,
            RoutineProgress routineProgress,
            Streak streak,
            NapService.ActiveNap activeNap,
            List<TopCoachingCard> topCoachingCards) {
        return new HomeDashboardResponse(
                date.toString(),
                scheduleSection.todayShift(),
                scheduleSection.nextShift(),
                scheduleSection.transitionDay(),
                routineProgress,
                topCoachingCards,
                streak,
                activeNap,
                scheduleSection.scheduleAlert());
    }

    public record TodayShift(String shiftType, String startTime, String endTime) {
    }

    public record NextShift(String workDate, String shiftType, long startsInMinutes) {
    }

    public record RoutineProgress(int total, int done, double completionRate) {
    }

    /**
     * @param windowEnd 이미 지난 시간대인지 앱이 판단해 흐리게 보여줄 수 있도록 함께 내려준다.
     */
    public record TopCoachingCard(Long cardId, String cardType, String title, String windowStart, String windowEnd) {
    }

    public record Streak(int currentStreak) {
    }

    public record ScheduleAlert(String type, int remainingDays) {
    }
}
