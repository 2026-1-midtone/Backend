package com.midtone.backend.coaching.application;

import com.midtone.backend.coaching.domain.CoachingCard;
import com.midtone.backend.coaching.domain.DailyCoaching;
import com.midtone.backend.global.time.DateTimeDefaults;
import java.time.LocalDateTime;
import java.util.List;

public record TodayCoachingResponse(
        Long coachingId,
        String coachingDate,
        String todayShiftType,
        String nextShiftStartAt,
        boolean isTransitionDay,
        List<Card> cards,
        String disclaimer) {

    private static final String DISCLAIMER = "본 코칭은 참고용이며 의학적 진단·치료를 대체하지 않습니다.";

    public static TodayCoachingResponse of(DailyCoaching dailyCoaching, List<CoachingCard> cards) {
        List<Card> cardResponses = cards.stream().map(Card::from).toList();
        return new TodayCoachingResponse(
                dailyCoaching.getId(),
                dailyCoaching.getCoachingDate().toString(),
                dailyCoaching.getTodayShiftType().name(),
                format(dailyCoaching.getNextShiftStartAt()),
                dailyCoaching.isTransitionDay(),
                cardResponses,
                DISCLAIMER);
    }

    private static String format(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(DateTimeDefaults.DEFAULT_ZONE).toOffsetDateTime().toString();
    }

    public record Card(
            Long cardId, String cardType, String title, String windowStart, String windowEnd, String description) {

        public static Card from(CoachingCard card) {
            return new Card(
                    card.getId(),
                    card.getCardType().name(),
                    card.getTitle(),
                    format(card.getWindowStart()),
                    format(card.getWindowEnd()),
                    card.getDescription());
        }
    }
}
