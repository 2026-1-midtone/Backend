package com.midtone.backend.caffeine.application;

import com.midtone.backend.caffeine.domain.CaffeineIntake;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 카페인 섭취 기록의 mg·잔 수 합산 로직을 한 곳에서 관리한다.
 * amountMg는 "1잔당 카페인 양", servings는 "그 기록에서 마신 잔 수"를 의미하므로,
 * 한 기록의 실제 섭취량은 amountMg * servings로 계산해야 한다.
 * (예: 100mg짜리 음료를 2잔 마셨으면 그 기록의 실제 섭취량은 200mg)
 */
final class CaffeineTotals {

    private CaffeineTotals() {
    }

    static int totalAmountMg(List<CaffeineIntake> intakes) {
        BigDecimal total = intakes.stream()
                .map(intake -> BigDecimal.valueOf(intake.getAmountMg()).multiply(intake.getServings()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    static BigDecimal totalServings(List<CaffeineIntake> intakes) {
        return intakes.stream()
                .map(CaffeineIntake::getServings)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }
}
