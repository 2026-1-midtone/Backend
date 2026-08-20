package com.midtone.backend.caffeine.application;

import com.midtone.backend.caffeine.domain.CaffeineIntake;
import com.midtone.backend.caffeine.domain.CaffeineIntakeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class CaffeineStatusCalculator {

    // 하루 300mg 이상 섭취 시 불안·불쾌감, 수면시간·수면효율 저하와 관련된다는 연구 근거
    // (Lee, Yoo, Lee, Park, & Kim, 2007 — 김혜성·이종은, 2020에서 재인용)에 따른 상한.
    private static final int DAILY_LIMIT_MG = 300;

    private final CaffeineIntakeRepository caffeineIntakeRepository;

    public CaffeineStatusCalculator(CaffeineIntakeRepository caffeineIntakeRepository) {
        this.caffeineIntakeRepository = caffeineIntakeRepository;
    }

    public DailyCaffeineStatus calculate(long userId, LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        List<CaffeineIntake> intakes = caffeineIntakeRepository
                .findByUserIdAndConsumedAtGreaterThanEqualAndConsumedAtLessThanOrderByConsumedAtAsc(
                        userId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());

        int totalAmountMg = CaffeineTotals.totalAmountMg(intakes);
        BigDecimal totalServings = CaffeineTotals.totalServings(intakes);

        return new DailyCaffeineStatus(
                date,
                totalAmountMg,
                totalServings,
                totalAmountMg >= DAILY_LIMIT_MG);
    }
}
