package com.midtone.backend.caffeine.application;

import com.midtone.backend.caffeine.domain.CaffeineIntake;
import com.midtone.backend.caffeine.domain.CaffeineIntakeRepository;
import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.user.domain.User;
import com.midtone.backend.user.domain.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaffeineIntakeService {

    private static final BigDecimal DEFAULT_SERVINGS = new BigDecimal("1.00");

    private final CurrentUserIdProvider currentUserIdProvider;
    private final CaffeineIntakeRepository caffeineIntakeRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public CaffeineIntakeService(
            CurrentUserIdProvider currentUserIdProvider,
            CaffeineIntakeRepository caffeineIntakeRepository,
            UserRepository userRepository,
            Clock clock) {
        this.currentUserIdProvider = currentUserIdProvider;
        this.caffeineIntakeRepository = caffeineIntakeRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public CaffeineIntakeResponse create(CreateCaffeineIntakeRequest request) {
        long userId = currentUserIdProvider.getCurrentUserId();
        ZoneId zone = userZone(userId);
        validateConsumedAt(request.consumedAt());
        validateAmount(request.amountMg());
        BigDecimal servings = normalizeServings(request.servings() == null ? DEFAULT_SERVINGS : request.servings());
        CaffeineIntake saved = caffeineIntakeRepository.save(new CaffeineIntake(
                userId,
                request.consumedAt().atZoneSameInstant(zone).toLocalDateTime(),
                zone.getId(),
                request.amountMg(),
                servings,
                request.beverageType()));
        return CaffeineIntakeResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public CaffeineIntakeListResponse getIntakes(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new CaffeineIntakeException(CaffeineIntakeException.ErrorCode.INVALID_RANGE);
        }
        long userId = currentUserIdProvider.getCurrentUserId();
        userZone(userId);
        List<CaffeineIntake> found = caffeineIntakeRepository
                .findByUserIdAndConsumedAtBetweenOrderByConsumedAtAsc(
                        userId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        int totalAmountMg = CaffeineTotals.totalAmountMg(found);
        BigDecimal totalServings = CaffeineTotals.totalServings(found);
        return new CaffeineIntakeListResponse(
                found.stream().map(CaffeineIntakeResponse::from).toList(),
                totalAmountMg,
                totalServings);
    }

    @Transactional
    public CaffeineIntakeResponse update(long intakeId, UpdateCaffeineIntakeRequest request) {
        long userId = currentUserIdProvider.getCurrentUserId();
        CaffeineIntake intake = ownedIntake(intakeId, userId);
        ZoneId zone = userZone(userId);
        OffsetDateTime consumedAt = request.consumedAt() == null
                ? intake.getConsumedAt().atZone(ZoneId.of(intake.getRecordedTimezone())).toOffsetDateTime()
                : request.consumedAt();
        int amountMg = request.amountMg() == null ? intake.getAmountMg() : request.amountMg();
        BigDecimal servings = request.servings() == null ? intake.getServings() : request.servings();
        validateConsumedAt(consumedAt);
        validateAmount(amountMg);
        servings = normalizeServings(servings);
        intake.update(
                consumedAt.atZoneSameInstant(zone).toLocalDateTime(),
                zone.getId(),
                amountMg,
                servings,
                request.beverageType() == null ? intake.getBeverageType() : request.beverageType());
        return CaffeineIntakeResponse.from(intake);
    }

    @Transactional
    public void delete(long intakeId) {
        long userId = currentUserIdProvider.getCurrentUserId();
        caffeineIntakeRepository.delete(ownedIntake(intakeId, userId));
    }

    private CaffeineIntake ownedIntake(long intakeId, long userId) {
        CaffeineIntake intake = caffeineIntakeRepository.findById(intakeId)
                .orElseThrow(() -> new CaffeineIntakeException(
                        CaffeineIntakeException.ErrorCode.INTAKE_NOT_FOUND));
        if (intake.getUserId() != userId) {
            throw new CaffeineIntakeException(CaffeineIntakeException.ErrorCode.ACCESS_DENIED);
        }
        return intake;
    }

    private ZoneId userZone(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CaffeineIntakeException(
                        CaffeineIntakeException.ErrorCode.USER_NOT_FOUND));
        return ZoneId.of(user.getTimezone());
    }

    private void validateConsumedAt(OffsetDateTime consumedAt) {
        if (consumedAt == null || consumedAt.toInstant().isAfter(clock.instant())) {
            throw new CaffeineIntakeException(CaffeineIntakeException.ErrorCode.FUTURE_RECORD);
        }
    }

    private void validateAmount(int amountMg) {
        if (amountMg <= 0) {
            throw new CaffeineIntakeException(CaffeineIntakeException.ErrorCode.INVALID_AMOUNT);
        }
    }

    private BigDecimal normalizeServings(BigDecimal servings) {
        if (servings == null || servings.signum() <= 0 || servings.scale() > 2 || servings.precision() - servings.scale() > 2) {
            throw new CaffeineIntakeException(CaffeineIntakeException.ErrorCode.INVALID_SERVINGS);
        }
        return servings.setScale(2, RoundingMode.UNNECESSARY);
    }
}
