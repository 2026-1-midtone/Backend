package com.midtone.backend.caffeine.application;

import com.midtone.backend.caffeine.domain.CaffeineIntake;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record CaffeineIntakeResponse(
        Long intakeId,
        OffsetDateTime consumedAt,
        String recordedTimezone,
        int amountMg,
        BigDecimal servings,
        String beverageType,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static CaffeineIntakeResponse from(CaffeineIntake intake) {
        ZoneId zone = ZoneId.of(intake.getRecordedTimezone());
        return new CaffeineIntakeResponse(
                intake.getId(),
                intake.getConsumedAt().atZone(zone).toOffsetDateTime(),
                intake.getRecordedTimezone(),
                intake.getAmountMg(),
                intake.getServings(),
                intake.getBeverageType(),
                intake.getCreatedAt().atZone(zone).toOffsetDateTime(),
                intake.getUpdatedAt().atZone(zone).toOffsetDateTime());
    }
}
