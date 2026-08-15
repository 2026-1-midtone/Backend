package com.midtone.backend.coaching.application;

import com.midtone.backend.coaching.domain.CoachingCard;
import com.midtone.backend.coaching.domain.DailyCoaching;
import com.midtone.backend.global.time.DateTimeDefaults;
import java.time.LocalDateTime;

public record CoachingCardDetailResponse(
        Long cardId,
        String cardType,
        String title,
        String windowStart,
        String windowEnd,
        String rationale,
        BasedOn basedOn) {

    public static CoachingCardDetailResponse of(CoachingCard card, DailyCoaching dailyCoaching) {
        BasedOn basedOn = new BasedOn(
                dailyCoaching.getTodayShiftType().name(),
                format(dailyCoaching.getNextShiftStartAt()),
                dailyCoaching.getCaffeineSensitivityUsed().name());
        return new CoachingCardDetailResponse(
                card.getId(),
                card.getCardType().name(),
                card.getTitle(),
                format(card.getWindowStart()),
                format(card.getWindowEnd()),
                card.getRationale(),
                basedOn);
    }

    private static String format(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(DateTimeDefaults.DEFAULT_ZONE).toOffsetDateTime().toString();
    }

    public record BasedOn(String todayShiftType, String nextShiftStartAt, String caffeineSensitivity) {
    }
}
