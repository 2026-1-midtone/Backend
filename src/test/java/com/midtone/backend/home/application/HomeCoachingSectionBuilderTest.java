package com.midtone.backend.home.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.midtone.backend.coaching.application.CoachingException;
import com.midtone.backend.coaching.application.CoachingService;
import com.midtone.backend.coaching.application.TodayCoachingResponse;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HomeCoachingSectionBuilderTest {

    @Mock
    private CoachingService coachingService;

    @InjectMocks
    private HomeCoachingSectionBuilder homeCoachingSectionBuilder;

    @Test
    void 이미_지난_카드는_제외하고_시작_시각순으로_반환한다() {
        LocalDate date = LocalDate.of(2026, 8, 9);
        OffsetDateTime now = OffsetDateTime.now();
        TodayCoachingResponse.Card pastCard = new TodayCoachingResponse.Card(
                1L, "LIGHT_EXPOSURE", "밝은 빛 노출", now.minusHours(3).toString(), now.minusHours(1).toString(), "설명");
        TodayCoachingResponse.Card laterCard = new TodayCoachingResponse.Card(
                2L, "CAFFEINE_CUTOFF", "카페인 컷오프", now.plusHours(2).toString(), now.plusHours(3).toString(), "설명");
        TodayCoachingResponse.Card soonerCard = new TodayCoachingResponse.Card(
                3L, "NAP", "권장 낮잠", now.plusMinutes(30).toString(), now.plusHours(1).toString(), "설명");
        TodayCoachingResponse response = new TodayCoachingResponse(
                10L, date.toString(), "NIGHT", null, false, List.of(pastCard, laterCard, soonerCard), "안내");
        when(coachingService.getTodayCoaching(date)).thenReturn(response);

        List<HomeDashboardResponse.TopCoachingCard> result = homeCoachingSectionBuilder.build(date);

        assertEquals(2, result.size());
        assertEquals(3L, result.get(0).cardId());
        assertEquals(2L, result.get(1).cardId());
    }

    @Test
    void 근무_일정이_없으면_빈_목록을_반환한다() {
        LocalDate date = LocalDate.of(2026, 8, 9);
        when(coachingService.getTodayCoaching(date))
                .thenThrow(new CoachingException(CoachingException.ErrorCode.SHIFT_NOT_REGISTERED));

        List<HomeDashboardResponse.TopCoachingCard> result = homeCoachingSectionBuilder.build(date);

        assertTrue(result.isEmpty());
    }
}
