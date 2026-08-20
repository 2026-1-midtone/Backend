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
        return selectUpcomingTopCards(fetchCards(date));
    }

    private List<TodayCoachingResponse.Card> fetchCards(LocalDate date) {
        try {
            return coachingService.getTodayCoaching(date).cards();
        } catch (CoachingException exception) {
            return List.of();
        }
    }

    private List<HomeDashboardResponse.TopCoachingCard> selectUpcomingTopCards(List<TodayCoachingResponse.Card> cards) {
        OffsetDateTime now = OffsetDateTime.now();
        return cards.stream()
                .filter(card -> isUpcoming(card, now))
                .sorted(Comparator.comparing(card -> OffsetDateTime.parse(card.windowStart())))
                .limit(MAX_TOP_CARDS)
                .map(this::toTopCard)
                .toList();
    }

    private boolean isUpcoming(TodayCoachingResponse.Card card, OffsetDateTime now) {
        return OffsetDateTime.parse(card.windowEnd()).isAfter(now);
    }

    private HomeDashboardResponse.TopCoachingCard toTopCard(TodayCoachingResponse.Card card) {
        return new HomeDashboardResponse.TopCoachingCard(card.cardId(), card.cardType(), card.title(), card.windowStart());
    }
}
