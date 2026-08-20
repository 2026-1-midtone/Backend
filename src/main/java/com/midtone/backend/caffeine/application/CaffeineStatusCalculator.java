package com.midtone.backend.caffeine.application;

import com.midtone.backend.caffeine.domain.CaffeineIntake;
import com.midtone.backend.caffeine.domain.CaffeineIntakeRepository;
import com.midtone.backend.user.domain.UserSettings;
import com.midtone.backend.user.domain.UserSettingsRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class CaffeineStatusCalculator {

    // 사용자가 개인화 설정에서 일일 카페인 섭취량(mg)을 지정하지 않았을 때 쓰는 기본 상한.
    // 화면의 "권장 입력 예시 200~400"의 상단값을 보수적인 기본값으로 사용한다.
    private static final int DEFAULT_DAILY_LIMIT_MG = 400;

    private final CaffeineIntakeRepository caffeineIntakeRepository;
    private final UserSettingsRepository userSettingsRepository;

    public CaffeineStatusCalculator(
            CaffeineIntakeRepository caffeineIntakeRepository, UserSettingsRepository userSettingsRepository) {
        this.caffeineIntakeRepository = caffeineIntakeRepository;
        this.userSettingsRepository = userSettingsRepository;
    }

    public DailyCaffeineStatus calculate(long userId, LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        List<CaffeineIntake> intakes = caffeineIntakeRepository
                .findByUserIdAndConsumedAtGreaterThanEqualAndConsumedAtLessThanOrderByConsumedAtAsc(
                        userId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());

        int totalAmountMg = intakes.stream().mapToInt(CaffeineIntake::getAmountMg).sum();
        BigDecimal totalServings = intakes.stream()
                .map(CaffeineIntake::getServings)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        int dailyLimitMg = resolveDailyLimitMg(userId);

        return new DailyCaffeineStatus(
                date,
                totalAmountMg,
                totalServings,
                dailyLimitMg,
                totalAmountMg > dailyLimitMg);
    }

    private int resolveDailyLimitMg(long userId) {
        return userSettingsRepository.findById(userId)
                .map(UserSettings::getCaffeineDailyMg)
                .filter(Objects::nonNull)
                .orElse(DEFAULT_DAILY_LIMIT_MG);
    }
}
