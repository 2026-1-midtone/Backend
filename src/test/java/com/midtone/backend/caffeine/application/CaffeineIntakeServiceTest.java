package com.midtone.backend.caffeine.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.midtone.backend.caffeine.domain.CaffeineIntake;
import com.midtone.backend.caffeine.domain.CaffeineIntakeRepository;
import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.user.domain.User;
import com.midtone.backend.user.domain.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CaffeineIntakeServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-18T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private CurrentUserIdProvider currentUserIdProvider;

    @Mock
    private CaffeineIntakeRepository caffeineIntakeRepository;

    @Mock
    private UserRepository userRepository;

    private CaffeineIntakeService service;

    @BeforeEach
    void setUp() {
        service = new CaffeineIntakeService(
                currentUserIdProvider, caffeineIntakeRepository, userRepository, CLOCK);
    }

    @Test
    void 잔_수를_생략하면_한_잔으로_저장한다() {
        given(currentUserIdProvider.getCurrentUserId()).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user()));
        given(caffeineIntakeRepository.save(any(CaffeineIntake.class))).willAnswer(invocation -> {
            CaffeineIntake saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 20L);
            return saved;
        });

        CaffeineIntakeResponse result = service.create(new CreateCaffeineIntakeRequest(
                OffsetDateTime.parse("2026-08-18T09:00:00+09:00"), 120, null, "COFFEE"));

        assertEquals(20L, result.intakeId());
        assertEquals(new BigDecimal("1.00"), result.servings());
        assertEquals(120, result.amountMg());
    }

    @Test
    void 카페인_용량이_양수가_아니면_거절한다() {
        given(currentUserIdProvider.getCurrentUserId()).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user()));

        CaffeineIntakeException error = assertThrows(CaffeineIntakeException.class, () -> service.create(
                new CreateCaffeineIntakeRequest(
                        OffsetDateTime.parse("2026-08-18T09:00:00+09:00"), 0,
                        new BigDecimal("1.00"), "COFFEE")));

        assertEquals(CaffeineIntakeException.ErrorCode.INVALID_AMOUNT, error.getErrorCode());
    }

    @Test
    void 잔_수가_양수가_아니면_거절한다() {
        given(currentUserIdProvider.getCurrentUserId()).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user()));

        CaffeineIntakeException error = assertThrows(CaffeineIntakeException.class, () -> service.create(
                new CreateCaffeineIntakeRequest(
                        OffsetDateTime.parse("2026-08-18T09:00:00+09:00"), 120,
                        BigDecimal.ZERO, "COFFEE")));

        assertEquals(CaffeineIntakeException.ErrorCode.INVALID_SERVINGS, error.getErrorCode());
    }

    @Test
    void 미래_섭취시각은_거절한다() {
        given(currentUserIdProvider.getCurrentUserId()).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user()));

        CaffeineIntakeException error = assertThrows(CaffeineIntakeException.class, () -> service.create(
                new CreateCaffeineIntakeRequest(
                        OffsetDateTime.parse("2026-08-19T09:00:00+09:00"), 120,
                        new BigDecimal("1.00"), "COFFEE")));

        assertEquals(CaffeineIntakeException.ErrorCode.FUTURE_RECORD, error.getErrorCode());
    }

    @Test
    void 기간_조회는_mg와_잔_수를_각각_합산한다() {
        given(currentUserIdProvider.getCurrentUserId()).willReturn(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user()));
        given(caffeineIntakeRepository.findByUserIdAndConsumedAtBetweenOrderByConsumedAtAsc(
                1L, LocalDateTime.parse("2026-08-18T00:00:00"), LocalDateTime.parse("2026-08-19T00:00:00")))
                .willReturn(List.of(intake(80, "1.00"), intake(120, "0.50")));

        CaffeineIntakeListResponse result = service.getIntakes(
                LocalDate.parse("2026-08-18"), LocalDate.parse("2026-08-18"));

        // 80mg×1.00잔 + 120mg×0.50잔 = 80 + 60 = 140mg
        assertEquals(140, result.totalAmountMg());
        assertEquals(new BigDecimal("1.50"), result.totalServings());
        assertEquals(2, result.intakes().size());
    }

    @Test
    void 타인의_섭취기록은_삭제할_수_없다() {
        given(currentUserIdProvider.getCurrentUserId()).willReturn(1L);
        given(caffeineIntakeRepository.findById(20L)).willReturn(Optional.of(intake(2L, 120, "1.00")));

        CaffeineIntakeException error = assertThrows(
                CaffeineIntakeException.class, () -> service.delete(20L));

        assertEquals(CaffeineIntakeException.ErrorCode.ACCESS_DENIED, error.getErrorCode());
    }

    private User user() {
        return new User("google-1", "user@example.com", "사용자", null);
    }

    private CaffeineIntake intake(int amountMg, String servings) {
        return intake(1L, amountMg, servings);
    }

    private CaffeineIntake intake(long userId, int amountMg, String servings) {
        CaffeineIntake intake = new CaffeineIntake(
                userId,
                LocalDateTime.parse("2026-08-18T09:00:00"),
                "Asia/Seoul",
                amountMg,
                new BigDecimal(servings),
                "COFFEE");
        ReflectionTestUtils.setField(intake, "id", 20L);
        return intake;
    }
}
