package com.midtone.backend.chat.application;

import com.midtone.backend.caffeine.application.DailyCaffeineStatus;
import com.midtone.backend.sleep.application.SleepPattern;
import java.time.LocalDate;
import java.util.List;

public record ChatContextSnapshot(
        LocalDate date,
        String schedule,
        SleepPattern sleepPattern,
        DailyCaffeineStatus caffeineStatus,
        String caffeineSensitivity,
        List<CoachingCardSnapshot> coachingCards,
        RoutineProgress routineProgress) {

    public static ChatContextSnapshot empty(LocalDate date) {
        return new ChatContextSnapshot(date, null, null, null, null, List.of(), new RoutineProgress(0, 0));
    }

    public record CoachingCardSnapshot(String type, String windowStart, String windowEnd, String description) {}
    public record RoutineProgress(int completed, int total) {}
}
