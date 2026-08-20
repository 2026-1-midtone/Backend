package com.midtone.backend.coaching.application;

import com.midtone.backend.coaching.domain.CoachingCard;
import com.midtone.backend.coaching.domain.CoachingCardRepository;
import com.midtone.backend.coaching.domain.DailyCoaching;
import com.midtone.backend.coaching.domain.DailyCoachingRepository;
import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.routine.application.RoutineTaskGenerator;
import com.midtone.backend.shift.application.schedule.NextShiftFinder;
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
    private final RoutineTaskGenerator routineTaskGenerator;
    private final NextShiftFinder nextShiftFinder;

    public CoachingService(
            CurrentUserIdProvider currentUserIdProvider,
            DailyCoachingGenerator dailyCoachingGenerator,
            DailyCoachingRepository dailyCoachingRepository,
            CoachingCardRepository coachingCardRepository,
            RoutineTaskGenerator routineTaskGenerator,
            NextShiftFinder nextShiftFinder) {
        this.currentUserIdProvider = currentUserIdProvider;
        this.dailyCoachingGenerator = dailyCoachingGenerator;
        this.dailyCoachingRepository = dailyCoachingRepository;
        this.coachingCardRepository = coachingCardRepository;
        this.routineTaskGenerator = routineTaskGenerator;
        this.nextShiftFinder = nextShiftFinder;
    }

    @Transactional
    public TodayCoachingResponse getTodayCoaching(LocalDate date) {
        long userId = currentUserIdProvider.getCurrentUserId();
        DailyCoaching dailyCoaching = dailyCoachingRepository.findByUserIdAndCoachingDate(userId, date)
                .map(cached -> healIfCardsMissing(userId, date, cached))
                .orElseGet(() -> generateAndSave(userId, date));
        List<CoachingCard> cards = coachingCardRepository.findByDailyCoachingId(dailyCoaching.getId());
        return TodayCoachingResponse.of(dailyCoaching, cards, nextShiftFinder.findStartAt(userId, date));
    }

    /**
     * 근무 시각이 비어 있던 시절에 카드 0장으로 캐시된 코칭은 근무표를 고쳐도 그대로 남아
     * 화면이 "해당 없음"으로만 보인다. 지금 다시 만들면 카드가 나오는 경우에만 갈아끼운다.
     * OFF처럼 원래 카드가 없는 날까지 매 조회마다 다시 쓰지 않기 위한 조건이다.
     */
    private DailyCoaching healIfCardsMissing(long userId, LocalDate date, DailyCoaching cached) {
        if (!coachingCardRepository.findByDailyCoachingId(cached.getId()).isEmpty()) {
            return cached;
        }
        return dailyCoachingGenerator.generate(userId, date)
                .filter(generated -> !generated.cards().isEmpty())
                .map(generated -> {
                    deleteExistingCoaching(userId, date);
                    return saveGeneratedCoaching(userId, date, generated);
                })
                .orElse(cached);
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
        List<CoachingCard> savedCards = saveCards(dailyCoaching.getId(), generated.cards());
        routineTaskGenerator.regenerate(userId, date, generated.todayShift(), savedCards);
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

    private List<CoachingCard> saveCards(Long dailyCoachingId, List<CoachingCard.CoachingCardContent> contents) {
        List<CoachingCard> cards = contents.stream()
                .map(content -> new CoachingCard(dailyCoachingId, content))
                .toList();
        return coachingCardRepository.saveAll(cards);
    }
}
