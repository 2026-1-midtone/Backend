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

    /**
     * 다음 근무 시작 시각은 저장된 값이 아니라 조회 시점에 다시 계산한 값을 쓴다.
     * 코칭은 날짜당 한 번만 만들어 두고 재사용하기 때문에, 그 뒤 근무표가 바뀌면
     * 저장된 값이 그대로 남아 홈 화면의 카운트다운과 어긋난다.
     */
    public static TodayCoachingResponse of(
            DailyCoaching dailyCoaching, List<CoachingCard> cards, LocalDateTime nextShiftStartAt) {
        List<Card> cardResponses = cards.stream().map(Card::from).toList();
        return new TodayCoachingResponse(
                dailyCoaching.getId(),
                dailyCoaching.getCoachingDate().toString(),
                dailyCoaching.getTodayShiftType().name(),
                format(nextShiftStartAt),
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
