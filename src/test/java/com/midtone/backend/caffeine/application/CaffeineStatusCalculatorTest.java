package com.midtone.backend.caffeine.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import com.midtone.backend.caffeine.domain.CaffeineIntake;
import com.midtone.backend.caffeine.domain.CaffeineIntakeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CaffeineStatusCalculatorTest {

    @Mock
    private CaffeineIntakeRepository caffeineIntakeRepository;

    private CaffeineStatusCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CaffeineStatusCalculator(caffeineIntakeRepository);
    }

    @Test
    void 하루_잔_수가_두_잔을_초과하면_경고한다() {
        LocalDate date = LocalDate.parse("2026-08-18");
        given(caffeineIntakeRepository.findByUserIdAndConsumedAtGreaterThanEqualAndConsumedAtLessThanOrderByConsumedAtAsc(
                1L, date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .willReturn(List.of(
                        intake("2026-08-18T09:00:00", 120, "1.25"),
                        intake("2026-08-18T13:00:00", 80, "1.00")));

        DailyCaffeineStatus result = calculator.calculate(1L, date);

        assertEquals(200, result.totalAmountMg());
        assertEquals(new BigDecimal("2.25"), result.totalServings());
        assertTrue(result.overServingLimit());
    }

    @Test
    void 정확히_두_잔은_경고하지_않는다() {
        LocalDate date = LocalDate.parse("2026-08-18");
        given(caffeineIntakeRepository.findByUserIdAndConsumedAtGreaterThanEqualAndConsumedAtLessThanOrderByConsumedAtAsc(
                1L, date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .willReturn(List.of(intake("2026-08-18T09:00:00", 200, "2.00")));

        DailyCaffeineStatus result = calculator.calculate(1L, date);

        assertFalse(result.overServingLimit());
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
