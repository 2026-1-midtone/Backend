package com.midtone.backend.caffeine.application;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyCaffeineStatus(
        LocalDate date,
        int totalAmountMg,
        BigDecimal totalServings,
        int dailyLimitMg,
        boolean overDailyLimit) {
}
