package com.midtone.backend.coaching.application;

import com.midtone.backend.coaching.domain.CoachingCard;
import com.midtone.backend.coaching.domain.CoachingCardRepository;
import com.midtone.backend.coaching.domain.DailyCoaching;
import com.midtone.backend.coaching.domain.DailyCoachingRepository;
import com.midtone.backend.global.user.CurrentUserIdProvider;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoachingService {

    private static final int MAX_REGENERATE_RANGE_DAYS = 90;

    private final CurrentUserIdProvider currentUserIdProvider;
    private final DailyCoachingGenerator dailyCoachingGenerator;
    private final DailyCoachingRepository dailyCoachingRepository;
    private final CoachingCardRepository coachingCardRepository;

    public CoachingService(
            CurrentUserIdProvider currentUserIdProvider,
            DailyCoachingGenerator dailyCoachingGenerator,
            DailyCoachingRepository dailyCoachingRepository,
            CoachingCardRepository coachingCardRepository) {
        this.currentUserIdProvider = currentUserIdProvider;
        this.dailyCoachingGenerator = dailyCoachingGenerator;
        this.dailyCoachingRepository = dailyCoachingRepository;
        this.coachingCardRepository = coachingCardRepository;
    }

    @Transactional
    public TodayCoachingResponse getTodayCoaching(LocalDate date) {
        long userId = currentUserIdProvider.getCurrentUserId();
        DailyCoaching dailyCoaching = dailyCoachingRepository.findByUserIdAndCoachingDate(userId, date)
                .orElseGet(() -> generateAndSave(userId, date));
        List<CoachingCard> cards = coachingCardRepository.findByDailyCoachingId(dailyCoaching.getId());
        return TodayCoachingResponse.of(dailyCoaching, cards);
    }

    @Transactional(readOnly = true)
    public CoachingCardDetailResponse getCardDetail(Long cardId) {
        CoachingCard card = coachingCardRepository.findById(cardId)
                .orElseThrow(() -> new CoachingException(CoachingException.ErrorCode.CARD_NOT_FOUND));
        DailyCoaching dailyCoaching = dailyCoachingRepository.findById(card.getDailyCoachingId())
                .orElseThrow(() -> new CoachingException(CoachingException.ErrorCode.CARD_NOT_FOUND));
        return CoachingCardDetailResponse.of(card, dailyCoaching);
    }

    @Transactional
    public RegenerateCoachingResponse regenerateCoaching(RegenerateCoachingRequest request) {
        LocalDate from = LocalDate.parse(request.from());
        LocalDate to = LocalDate.parse(request.to());
        validateRegenerateRange(from, to);
        long userId = currentUserIdProvider.getCurrentUserId();
        List<String> regeneratedDates = regenerateRange(userId, from, to);
        return new RegenerateCoachingResponse(regeneratedDates, regeneratedDates.size());
    }

    private void validateRegenerateRange(LocalDate from, LocalDate to) {
        if (ChronoUnit.DAYS.between(from, to) > MAX_REGENERATE_RANGE_DAYS) {
            throw new CoachingException(CoachingException.ErrorCode.REGENERATE_RANGE_EXCEEDED);
        }
    }

    private List<String> regenerateRange(long userId, LocalDate from, LocalDate to) {
        List<String> regeneratedDates = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (regenerateOneDay(userId, date)) {
                regeneratedDates.add(date.toString());
            }
        }
        return regeneratedDates;
    }

    private boolean regenerateOneDay(long userId, LocalDate date) {
        Optional<GeneratedCoaching> generated = dailyCoachingGenerator.generate(userId, date);
        if (generated.isEmpty()) {
            return false;
        }
        deleteExistingCoaching(userId, date);
        saveGeneratedCoaching(userId, date, generated.get());
        return true;
    }

    private void deleteExistingCoaching(long userId, LocalDate date) {
        dailyCoachingRepository.findByUserIdAndCoachingDate(userId, date).ifPresent(existing -> {
            coachingCardRepository.deleteByDailyCoachingId(existing.getId());
            dailyCoachingRepository.delete(existing);
            dailyCoachingRepository.flush();
        });
    }

    private DailyCoaching generateAndSave(long userId, LocalDate date) {
        GeneratedCoaching generated = dailyCoachingGenerator.generate(userId, date)
                .orElseThrow(() -> new CoachingException(CoachingException.ErrorCode.SHIFT_NOT_REGISTERED));
        return saveGeneratedCoaching(userId, date, generated);
    }

    private DailyCoaching saveGeneratedCoaching(long userId, LocalDate date, GeneratedCoaching generated) {
        DailyCoaching dailyCoaching = new DailyCoaching(userId, toContent(date, generated));
        dailyCoachingRepository.save(dailyCoaching);
        saveCards(dailyCoaching.getId(), generated.cards());
        return dailyCoaching;
    }

    private DailyCoaching.DailyCoachingContent toContent(LocalDate date, GeneratedCoaching generated) {
        return new DailyCoaching.DailyCoachingContent(
                date,
                generated.todayShift().getShiftType(),
                generated.nextShiftStartAt(),
                generated.caffeineSensitivity(),
                generated.transitionDay());
    }

    private void saveCards(Long dailyCoachingId, List<CoachingCard.CoachingCardContent> contents) {
        List<CoachingCard> cards = contents.stream()
                .map(content -> new CoachingCard(dailyCoachingId, content))
                .toList();
        coachingCardRepository.saveAll(cards);
    }
}
