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
    void 기록별_mg에_잔수를_곱해서_하루_총량을_계산한다() {
        // 11시 100mg 1잔(100mg) + 12시 100mg 2잔(200mg) + 15시 50mg 1잔(50mg) = 350mg
        LocalDate date = LocalDate.parse("2026-08-21");
        given(caffeineIntakeRepository.findByUserIdAndConsumedAtGreaterThanEqualAndConsumedAtLessThanOrderByConsumedAtAsc(
                1L, date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .willReturn(List.of(
                        intake("2026-08-21T11:00:00", 100, "1.00"),
                        intake("2026-08-21T12:00:00", 100, "2.00"),
                        intake("2026-08-21T15:00:00", 50, "1.00")));

        DailyCaffeineStatus result = calculator.calculate(1L, date);

        assertEquals(350, result.totalAmountMg());
        assertEquals(new BigDecimal("4.00"), result.totalServings());
        assertTrue(result.overDailyLimit());
    }

    @Test
    void 하루_섭취량이_300mg_미만이면_경고하지_않는다() {
        LocalDate date = LocalDate.parse("2026-08-18");
        given(caffeineIntakeRepository.findByUserIdAndConsumedAtGreaterThanEqualAndConsumedAtLessThanOrderByConsumedAtAsc(
                1L, date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .willReturn(List.of(intake("2026-08-18T09:00:00", 149, "2.00")));

        DailyCaffeineStatus result = calculator.calculate(1L, date);

        assertEquals(298, result.totalAmountMg());
        assertFalse(result.overDailyLimit());
    }

    @Test
    void 정확히_300mg이면_경고한다() {
        LocalDate date = LocalDate.parse("2026-08-18");
        given(caffeineIntakeRepository.findByUserIdAndConsumedAtGreaterThanEqualAndConsumedAtLessThanOrderByConsumedAtAsc(
                1L, date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .willReturn(List.of(intake("2026-08-18T09:00:00", 150, "2.00")));

        DailyCaffeineStatus result = calculator.calculate(1L, date);

        assertEquals(300, result.totalAmountMg());
        assertTrue(result.overDailyLimit());
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
