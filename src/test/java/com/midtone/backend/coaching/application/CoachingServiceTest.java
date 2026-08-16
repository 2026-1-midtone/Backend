package com.midtone.backend.coaching.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.midtone.backend.coaching.domain.CoachingCard.CoachingCardContent;
import com.midtone.backend.coaching.domain.CoachingCardRepository;
import com.midtone.backend.coaching.domain.CoachingCardType;
import com.midtone.backend.coaching.domain.DailyCoaching;
import com.midtone.backend.coaching.domain.DailyCoachingRepository;
import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
import com.midtone.backend.user.domain.CaffeineSensitivity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CoachingServiceTest {

    @Mock
    private CurrentUserIdProvider currentUserIdProvider;
    @Mock
    private DailyCoachingGenerator dailyCoachingGenerator;
    @Mock
    private DailyCoachingRepository dailyCoachingRepository;
    @Mock
    private CoachingCardRepository coachingCardRepository;

    @InjectMocks
    private CoachingService coachingService;

    @Test
    void 저장된_코칭이_있으면_그대로_반환한다() {
        LocalDate date = LocalDate.of(2026, 8, 7);
        DailyCoaching dailyCoaching = new DailyCoaching(1L, new DailyCoaching.DailyCoachingContent(
                date, ShiftType.NIGHT, null, CaffeineSensitivity.HIGH, false));
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(dailyCoachingRepository.findByUserIdAndCoachingDate(1L, date)).thenReturn(Optional.of(dailyCoaching));
        when(coachingCardRepository.findByDailyCoachingId(dailyCoaching.getId())).thenReturn(List.of());

        TodayCoachingResponse response = coachingService.getTodayCoaching(date);

        assertEquals("2026-08-07", response.coachingDate());
        assertEquals("NIGHT", response.todayShiftType());
    }

    @Test
    void 저장된_코칭이_없으면_생성해서_저장한다() {
        LocalDate date = LocalDate.of(2026, 8, 7);
        ShiftSchedule shift = new ShiftSchedule(
                1L, date, ShiftType.NIGHT, new ShiftTime(LocalTime.of(22, 0), LocalTime.of(7, 0)));
        CoachingCardContent cardContent = new CoachingCardContent(
                CoachingCardType.NAP, "권장 낮잠", LocalDateTime.of(2026, 8, 7, 18, 0),
                LocalDateTime.of(2026, 8, 7, 18, 20), "설명", "근거");
        GeneratedCoaching generated =
                new GeneratedCoaching(shift, null, false, CaffeineSensitivity.HIGH, List.of(cardContent));
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(dailyCoachingRepository.findByUserIdAndCoachingDate(1L, date)).thenReturn(Optional.empty());
        when(dailyCoachingGenerator.generate(1L, date)).thenReturn(Optional.of(generated));
        when(coachingCardRepository.findByDailyCoachingId(null)).thenReturn(List.of());

        TodayCoachingResponse response = coachingService.getTodayCoaching(date);

        assertEquals("NIGHT", response.todayShiftType());
    }

    @Test
    void 근무_일정이_없으면_예외를_던진다() {
        LocalDate date = LocalDate.of(2026, 8, 7);
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(dailyCoachingRepository.findByUserIdAndCoachingDate(1L, date)).thenReturn(Optional.empty());
        when(dailyCoachingGenerator.generate(1L, date)).thenReturn(Optional.empty());

        CoachingException exception =
                assertThrows(CoachingException.class, () -> coachingService.getTodayCoaching(date));
        assertEquals(CoachingException.ErrorCode.SHIFT_NOT_REGISTERED, exception.getErrorCode());
    }

    @Test
    void 존재하지_않는_카드를_조회하면_예외를_던진다() {
        when(coachingCardRepository.findById(999L)).thenReturn(Optional.empty());

        CoachingException exception =
                assertThrows(CoachingException.class, () -> coachingService.getCardDetail(999L));
        assertEquals(CoachingException.ErrorCode.CARD_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 기간_내_근무일만_코칭을_재생성한다() {
        RegenerateCoachingRequest request = new RegenerateCoachingRequest("2026-08-10", "2026-08-11");
        ShiftSchedule shiftOn10th = new ShiftSchedule(
                1L, LocalDate.of(2026, 8, 10), ShiftType.NIGHT, new ShiftTime(LocalTime.of(22, 0), LocalTime.of(7, 0)));
        GeneratedCoaching generatedFor10th =
                new GeneratedCoaching(shiftOn10th, null, false, CaffeineSensitivity.MEDIUM, List.of());
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(dailyCoachingGenerator.generate(1L, LocalDate.of(2026, 8, 10))).thenReturn(Optional.of(generatedFor10th));
        when(dailyCoachingGenerator.generate(1L, LocalDate.of(2026, 8, 11))).thenReturn(Optional.empty());
        when(dailyCoachingRepository.findByUserIdAndCoachingDate(1L, LocalDate.of(2026, 8, 10)))
                .thenReturn(Optional.empty());

        RegenerateCoachingResponse response = coachingService.regenerateCoaching(request);

        assertEquals(1, response.regeneratedCount());
        assertEquals(List.of("2026-08-10"), response.regeneratedDates());
    }

    @Test
    void 기존_코칭이_있으면_삭제하고_다시_생성한다() {
        RegenerateCoachingRequest request = new RegenerateCoachingRequest("2026-08-10", "2026-08-10");
        ShiftSchedule shift = new ShiftSchedule(
                1L, LocalDate.of(2026, 8, 10), ShiftType.NIGHT, new ShiftTime(LocalTime.of(22, 0), LocalTime.of(7, 0)));
        GeneratedCoaching generated = new GeneratedCoaching(shift, null, false, CaffeineSensitivity.MEDIUM, List.of());
        DailyCoaching existing = new DailyCoaching(1L, new DailyCoaching.DailyCoachingContent(
                LocalDate.of(2026, 8, 10), ShiftType.NIGHT, null, CaffeineSensitivity.MEDIUM, false));
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(dailyCoachingGenerator.generate(1L, LocalDate.of(2026, 8, 10))).thenReturn(Optional.of(generated));
        when(dailyCoachingRepository.findByUserIdAndCoachingDate(1L, LocalDate.of(2026, 8, 10)))
                .thenReturn(Optional.of(existing));

        coachingService.regenerateCoaching(request);

        verify(coachingCardRepository).deleteByDailyCoachingId(existing.getId());
        verify(dailyCoachingRepository).delete(existing);
    }

    @Test
    void 기존_코칭_삭제를_새_코칭_저장보다_먼저_반영한다() {
        RegenerateCoachingRequest request = new RegenerateCoachingRequest("2026-08-10", "2026-08-10");
        ShiftSchedule shift = new ShiftSchedule(
                1L, LocalDate.of(2026, 8, 10), ShiftType.NIGHT, new ShiftTime(LocalTime.of(22, 0), LocalTime.of(7, 0)));
        GeneratedCoaching generated = new GeneratedCoaching(shift, null, false, CaffeineSensitivity.MEDIUM, List.of());
        DailyCoaching existing = new DailyCoaching(1L, new DailyCoaching.DailyCoachingContent(
                LocalDate.of(2026, 8, 10), ShiftType.NIGHT, null, CaffeineSensitivity.MEDIUM, false));
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(1L);
        when(dailyCoachingGenerator.generate(1L, LocalDate.of(2026, 8, 10))).thenReturn(Optional.of(generated));
        when(dailyCoachingRepository.findByUserIdAndCoachingDate(1L, LocalDate.of(2026, 8, 10)))
                .thenReturn(Optional.of(existing));

        coachingService.regenerateCoaching(request);

        InOrder inOrder = inOrder(dailyCoachingRepository);
        inOrder.verify(dailyCoachingRepository).delete(existing);
        inOrder.verify(dailyCoachingRepository).flush();
        inOrder.verify(dailyCoachingRepository).save(any(DailyCoaching.class));
    }

    @Test
    void 재생성_기간이_90일을_초과하면_예외를_던진다() {
        RegenerateCoachingRequest request = new RegenerateCoachingRequest("2026-01-01", "2026-06-01");

        CoachingException exception =
                assertThrows(CoachingException.class, () -> coachingService.regenerateCoaching(request));
        assertEquals(CoachingException.ErrorCode.REGENERATE_RANGE_EXCEEDED, exception.getErrorCode());
    }
}
