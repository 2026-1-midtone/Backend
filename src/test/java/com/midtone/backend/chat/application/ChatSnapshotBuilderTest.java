package com.midtone.backend.chat.application;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.BDDMockito.given;

import com.midtone.backend.caffeine.application.CaffeineStatusCalculator;
import com.midtone.backend.caffeine.application.DailyCaffeineStatus;
import com.midtone.backend.coaching.domain.CoachingCardRepository;
import com.midtone.backend.coaching.domain.DailyCoachingRepository;
import com.midtone.backend.nutrition.application.NutrientNeedResponse;
import com.midtone.backend.nutrition.application.NutrientNeedService;
import com.midtone.backend.nutrition.application.NutritionRecommendationResponse;
import com.midtone.backend.nutrition.application.NutritionRecommendationService;
import com.midtone.backend.routine.domain.RoutineTaskRepository;
import com.midtone.backend.sleep.application.SleepPattern;
import com.midtone.backend.sleep.application.SleepPatternCalculator;
import com.midtone.backend.user.domain.UserSettingsRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatSnapshotBuilderTest {
    @Mock ChatContextBuilder contextBuilder;
    @Mock SleepPatternCalculator sleepCalculator;
    @Mock CaffeineStatusCalculator caffeineCalculator;
    @Mock DailyCoachingRepository dailyRepository;
    @Mock CoachingCardRepository cardRepository;
    @Mock RoutineTaskRepository routineRepository;
    @Mock UserSettingsRepository settingsRepository;
    @Mock NutrientNeedService nutrientNeedService;
    @Mock NutritionRecommendationService recommendationService;

    @Test
    void 영양소_목표와_백엔드_제품_추천을_채팅_스냅샷에_넣는다() {
        LocalDate date = LocalDate.parse("2026-08-18");
        NutrientNeedResponse needs = new NutrientNeedResponse(List.of(
                new NutrientNeedResponse.Item("VITAMIN_D", "HEALTH_CHECK", date)));
        NutritionRecommendationResponse recommendations = new NutritionRecommendationResponse(List.of(
                new NutritionRecommendationResponse.Recommendation(3L, "REVIVE_ENERGY_SHOT", "바이브젠 리바이브 에너지 샷",
                        "VIVEGEN REVIVE ENERGY SHOT", List.of("VITAMIN_D"), List.of(), "의약품이 아닙니다.")));
        given(contextBuilder.build(1L, date)).willReturn("schedule");
        given(sleepCalculator.calculate(1L, date)).willReturn(new SleepPattern(null, null, null, null, 0, date, date));
        given(caffeineCalculator.calculate(1L, date)).willReturn(new DailyCaffeineStatus(date, 0, new BigDecimal("0.00"), false));
        given(dailyRepository.findByUserIdAndCoachingDate(1L, date)).willReturn(Optional.empty());
        given(routineRepository.findAllByUserIdAndTaskDateOrderByIdAsc(1L, date)).willReturn(List.of());
        given(settingsRepository.findById(1L)).willReturn(Optional.empty());
        given(nutrientNeedService.get(1L)).willReturn(needs);
        given(recommendationService.getRecommendations(1L)).willReturn(recommendations);
        ChatSnapshotBuilder builder = new ChatSnapshotBuilder(contextBuilder, sleepCalculator, caffeineCalculator,
                dailyRepository, cardRepository, routineRepository, settingsRepository,
                nutrientNeedService, recommendationService);

        ChatContextSnapshot snapshot = builder.build(1L, date);

        assertSame(needs, snapshot.nutrientNeeds());
        assertSame(recommendations, snapshot.nutritionRecommendations());
    }
}
