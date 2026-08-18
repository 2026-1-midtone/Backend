package com.midtone.backend.caffeine.application;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record UpdateCaffeineIntakeRequest(
        OffsetDateTime consumedAt,
        @Positive(message = "amountMg는 1 이상이어야 합니다.") Integer amountMg,
        @DecimalMin(value = "0.01", message = "servings는 0보다 커야 합니다.")
        @Digits(integer = 2, fraction = 2, message = "servings는 소수점 둘째 자리까지 입력할 수 있습니다.")
        BigDecimal servings,
        @Size(max = 50, message = "beverageType은 50자를 초과할 수 없습니다.") String beverageType) {
}
