package com.midtone.backend.caffeine.application;

import java.math.BigDecimal;
import java.util.List;

public record CaffeineIntakeListResponse(
        List<CaffeineIntakeResponse> intakes,
        int totalAmountMg,
        BigDecimal totalServings) {
}
