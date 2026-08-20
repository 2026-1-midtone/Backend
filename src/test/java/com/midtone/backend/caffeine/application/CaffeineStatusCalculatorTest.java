package com.midtone.backend.caffeine.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import com.midtone.backend.caffeine.domain.CaffeineIntake;
import com.midtone.backend.caffeine.domain.CaffeineIntakeRepository;
import com.midtone.backend.user.domain.UserSettings;
import com.midtone.backend.user.domain.UserSettingsRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CaffeineStatusCalculatorTest {

    @Mock
    private CaffeineIntakeRepository caffeineIntakeRepository;

    @Mock
    private UserSettingsRepository userSettingsRepository;

    private CaffeineStatusCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CaffeineStatusCalculator(caffeineIntakeRepository, userSettingsRepository);
    }

    @Test
    void 개인화_설정한_일일_상한을_넘으면_경고한다() {
        LocalDate date = LocalDate.parse("2026-08-18");
        given(caffeineIntakeRepository.findByUserIdAndConsumedAtGreaterThanEqualAndConsumedAtLessThanOrderByConsumedAtAsc(
                1L, date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .willReturn(List.of(
                        intake("2026-08-18T09:00:00", 120, "1.25"),
                        intake("2026-08-18T13:00:00", 80, "1.00")));
        given(userSettingsRepository.findById(1L)).willReturn(Optional.of(settingsWithDailyLimit(150)));

        DailyCaffeineStatus result = calculator.calculate(1L, date);

        assertEquals(200, result.totalAmountMg());
        assertEquals(new BigDecimal("2.25"), result.totalServings());
        assertEquals(150, result.dailyLimitMg());
        assertTrue(result.overDailyLimit());
    }

    @Test
    void 일일_상한_이내면_경고하지_않는다() {
        LocalDate date = LocalDate.parse("2026-08-18");
        given(caffeineIntakeRepository.findByUserIdAndConsumedAtGreaterThanEqualAndConsumedAtLessThanOrderByConsumedAtAsc(
                1L, date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .willReturn(List.of(intake("2026-08-18T09:00:00", 200, "2.00")));
        given(userSettingsRepository.findById(1L)).willReturn(Optional.of(settingsWithDailyLimit(300)));

        DailyCaffeineStatus result = calculator.calculate(1L, date);

        assertEquals(300, result.dailyLimitMg());
        assertFalse(result.overDailyLimit());
    }

    @Test
    void 개인화_설정이_없으면_기본_상한_400mg을_쓴다() {
        LocalDate date = LocalDate.parse("2026-08-18");
        given(caffeineIntakeRepository.findByUserIdAndConsumedAtGreaterThanEqualAndConsumedAtLessThanOrderByConsumedAtAsc(
                1L, date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .willReturn(List.of(intake("2026-08-18T09:00:00", 450, "3.00")));
        given(userSettingsRepository.findById(1L)).willReturn(Optional.empty());

        DailyCaffeineStatus result = calculator.calculate(1L, date);

        assertEquals(400, result.dailyLimitMg());
        assertTrue(result.overDailyLimit());
    }

    private UserSettings settingsWithDailyLimit(int mg) {
        UserSettings settings = new UserSettings(
                1L, UserSettings.DEFAULT_PREFERRED_NAP_MINUTES, UserSettings.DEFAULT_MAX_NAPS_PER_DAY);
        settings.changeCaffeineDailyMg(mg);
        return settings;
    }

    private CaffeineIntake intake(String consumedAt, int amountMg, String servings) {
        return new CaffeineIntake(
                1L,
                LocalDateTime.parse(consumedAt),
                "Asia/Seoul",
                amountMg,
                new BigDecimal(servings),
                "COFFEE");
    }
}
