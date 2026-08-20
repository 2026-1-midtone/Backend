package com.midtone.backend.chat.application;

import com.midtone.backend.caffeine.application.CaffeineStatusCalculator;
import com.midtone.backend.caffeine.domain.CaffeineIntake;
import com.midtone.backend.caffeine.domain.CaffeineIntakeRepository;
import com.midtone.backend.coaching.domain.CoachingCardRepository;
import com.midtone.backend.coaching.domain.DailyCoachingRepository;
import com.midtone.backend.routine.domain.RoutineTaskRepository;
import com.midtone.backend.nutrition.application.NutrientNeedService;
import com.midtone.backend.nutrition.application.NutritionRecommendationService;
import com.midtone.backend.routine.domain.TaskStatus;
import com.midtone.backend.sleep.application.SleepPatternCalculator;
import com.midtone.backend.sleep.domain.SleepLog;
import com.midtone.backend.sleep.domain.SleepLogRepository;
import com.midtone.backend.user.domain.UserSettingsRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class ChatSnapshotBuilder {
    private final ChatContextBuilder scheduleBuilder;
    private final SleepPatternCalculator sleepCalculator;
    private final CaffeineStatusCalculator caffeineCalculator;
    private final DailyCoachingRepository dailyRepository;
    private final CoachingCardRepository cardRepository;
    private final RoutineTaskRepository routineRepository;
    private final SleepLogRepository sleepLogRepository;
    private final CaffeineIntakeRepository caffeineIntakeRepository;
    private final UserSettingsRepository settingsRepository;
    private final NutrientNeedService nutrientNeedService;
    private final NutritionRecommendationService nutritionRecommendationService;

    public ChatSnapshotBuilder(ChatContextBuilder scheduleBuilder, SleepPatternCalculator sleepCalculator,
            CaffeineStatusCalculator caffeineCalculator, DailyCoachingRepository dailyRepository,
            CoachingCardRepository cardRepository, RoutineTaskRepository routineRepository,
            SleepLogRepository sleepLogRepository, CaffeineIntakeRepository caffeineIntakeRepository,
            UserSettingsRepository settingsRepository, NutrientNeedService nutrientNeedService,
            NutritionRecommendationService nutritionRecommendationService) {
        this.scheduleBuilder = scheduleBuilder; this.sleepCalculator = sleepCalculator;
        this.caffeineCalculator = caffeineCalculator; this.dailyRepository = dailyRepository;
        this.cardRepository = cardRepository; this.routineRepository = routineRepository;
        this.sleepLogRepository = sleepLogRepository;
        this.caffeineIntakeRepository = caffeineIntakeRepository;
        this.settingsRepository = settingsRepository;
        this.nutrientNeedService = nutrientNeedService;
        this.nutritionRecommendationService = nutritionRecommendationService;
    }

    public ChatContextSnapshot build(long userId, LocalDate date) {
        var cards = dailyRepository.findByUserIdAndCoachingDate(userId, date)
                .map(daily -> cardRepository.findByDailyCoachingId(daily.getId()).stream()
                        .map(card -> new ChatContextSnapshot.CoachingCardSnapshot(card.getCardType().name(),
                                card.getWindowStart().toString(), card.getWindowEnd().toString(), card.getDescription()))
                        .toList()).orElseGet(java.util.List::of);
        var tasks = routineRepository.findAllByUserIdAndTaskDateOrderByIdAsc(userId, date);
        int completed = (int) tasks.stream().filter(task -> task.getStatus() == TaskStatus.DONE).count();
        String sensitivity = settingsRepository.findById(userId)
                .map(settings -> settings.getCaffeineSensitivity() == null ? null : settings.getCaffeineSensitivity().name())
                .orElse(null);
        return new ChatContextSnapshot(date, scheduleBuilder.build(userId, date), sleepCalculator.calculate(userId, date),
                recentSleepLogs(userId, date),
                caffeineCalculator.calculate(userId, date), recentCaffeineIntakes(userId, date), sensitivity, cards,
                new ChatContextSnapshot.RoutineProgress(completed, tasks.size()),
                nutrientNeedService.get(userId), nutritionRecommendationService.getRecommendations(userId));
    }

    private static final int RECENT_SLEEP_WINDOW_DAYS = 7;
    private static final int RECENT_SLEEP_MAX_LOGS = 5;
    private static final int RECENT_CAFFEINE_WINDOW_DAYS = 7;
    private static final int RECENT_CAFFEINE_MAX_LOGS = 5;

    private java.util.List<ChatContextSnapshot.RecentSleepLog> recentSleepLogs(long userId, LocalDate date) {
        java.util.List<SleepLog> logs = sleepLogRepository.findByUserIdAndWokeAtBetweenOrderByWokeAtAsc(
                userId, date.minusDays(RECENT_SLEEP_WINDOW_DAYS).atStartOfDay(), date.plusDays(1).atStartOfDay());
        return logs.reversed().stream()
                .limit(RECENT_SLEEP_MAX_LOGS)
                .map(log -> new ChatContextSnapshot.RecentSleepLog(
                        log.getSleptAt().toString(), log.getWokeAt().toString(),
                        java.time.Duration.between(log.getSleptAt(), log.getWokeAt()).toMinutes()))
                .toList();
    }

    private java.util.List<ChatContextSnapshot.RecentCaffeineIntake> recentCaffeineIntakes(long userId, LocalDate date) {
        java.util.List<CaffeineIntake> intakes = caffeineIntakeRepository.findByUserIdAndConsumedAtBetweenOrderByConsumedAtAsc(
                userId, date.minusDays(RECENT_CAFFEINE_WINDOW_DAYS).atStartOfDay(), date.plusDays(1).atStartOfDay());
        return intakes.reversed().stream()
                .limit(RECENT_CAFFEINE_MAX_LOGS)
                .map(intake -> new ChatContextSnapshot.RecentCaffeineIntake(
                        intake.getConsumedAt().toString(), intake.getAmountMg(), intake.getServings(),
                        intake.getBeverageType()))
                .toList();
    }
}
