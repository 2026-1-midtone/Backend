package com.midtone.backend.home.application;

import com.midtone.backend.coaching.application.CoachingException;
import com.midtone.backend.coaching.application.CoachingService;
import com.midtone.backend.coaching.application.TodayCoachingResponse;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class HomeCoachingSectionBuilder {

    private static final int MAX_TOP_CARDS = 3;

    private final CoachingService coachingService;

    public HomeCoachingSectionBuilder(CoachingService coachingService) {
        this.coachingService = coachingService;
    }

    public List<HomeDashboardResponse.TopCoachingCard> build(LocalDate date) {
        return selectTopCards(fetchCards(date));
    }

    private List<TodayCoachingResponse.Card> fetchCards(LocalDate date) {
        try {
            return coachingService.getTodayCoaching(date).cards();
        } catch (CoachingException exception) {
            return List.of();
        }
    }

    /**
     * 아직 남은 시간대를 먼저 보여주되, 이미 지난 시간대도 함께 준다.
     * 늦은 시간에 홈 화면의 코칭 영역이 통째로 비어 보이지 않게 하려는 것이다.
     */
    private List<HomeDashboardResponse.TopCoachingCard> selectTopCards(List<TodayCoachingResponse.Card> cards) {
        OffsetDateTime now = OffsetDateTime.now();
        return cards.stream()
                .sorted(Comparator.comparing((TodayCoachingResponse.Card card) -> isPast(card, now))
                        .thenComparing(card -> OffsetDateTime.parse(card.windowStart())))
                .limit(MAX_TOP_CARDS)
                .map(this::toTopCard)
                .toList();
    }

    private boolean isPast(TodayCoachingResponse.Card card, OffsetDateTime now) {
        return !OffsetDateTime.parse(card.windowEnd()).isAfter(now);
    }

    private HomeDashboardResponse.TopCoachingCard toTopCard(TodayCoachingResponse.Card card) {
        return new HomeDashboardResponse.TopCoachingCard(
                card.cardId(), card.cardType(), card.title(), card.windowStart(), card.windowEnd());
    }
}
