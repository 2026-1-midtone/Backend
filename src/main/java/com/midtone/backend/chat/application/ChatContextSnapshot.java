package com.midtone.backend.chat.application;

import com.midtone.backend.caffeine.application.DailyCaffeineStatus;
import com.midtone.backend.sleep.application.SleepPattern;
import com.midtone.backend.nutrition.application.NutrientNeedResponse;
import com.midtone.backend.nutrition.application.NutritionRecommendationResponse;
import java.time.LocalDate;
import java.util.List;

public record ChatContextSnapshot(
        LocalDate date,
        String schedule,
        SleepPattern sleepPattern,
        List<RecentSleepLog> recentSleepLogs,
        DailyCaffeineStatus caffeineStatus,
        String caffeineSensitivity,
        List<CoachingCardSnapshot> coachingCards,
        RoutineProgress routineProgress,
        NutrientNeedResponse nutrientNeeds,
        NutritionRecommendationResponse nutritionRecommendations) {

    public static ChatContextSnapshot empty(LocalDate date) {
        return new ChatContextSnapshot(date, null, null, List.of(), null, null, List.of(), new RoutineProgress(0, 0),
                new NutrientNeedResponse(List.of()), new NutritionRecommendationResponse(List.of()));
    }

    public record CoachingCardSnapshot(String type, String windowStart, String windowEnd, String description) {}
    public record RecentSleepLog(String sleptAt, String wokeAt, long durationMinutes) {}
    public record RoutineProgress(int completed, int total) {}
}
