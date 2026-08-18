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

    private static final BigDecimal SERVING_LIMIT = new BigDecimal("2.00");

    private final CaffeineIntakeRepository caffeineIntakeRepository;

    public CaffeineStatusCalculator(CaffeineIntakeRepository caffeineIntakeRepository) {
        this.caffeineIntakeRepository = caffeineIntakeRepository;
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

        return new DailyCaffeineStatus(
                date,
                totalAmountMg,
                totalServings,
                totalServings.compareTo(SERVING_LIMIT) > 0);
    }
}
