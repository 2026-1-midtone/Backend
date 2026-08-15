package com.midtone.backend.coaching.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.midtone.backend.coaching.domain.CoachingCard.CoachingCardContent;
import com.midtone.backend.coaching.domain.CoachingCardType;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
import com.midtone.backend.user.domain.CaffeineSensitivity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CoachingCardGeneratorTest {

    private final CoachingCardGenerator generator = new CoachingCardGenerator();

    @Test
    void 나이트_근무_기준으로_세_카드를_계산한다() {
        ShiftSchedule nightShift = new ShiftSchedule(
                1L, LocalDate.of(2026, 8, 7), ShiftType.NIGHT,
                new ShiftTime(LocalTime.of(22, 0), LocalTime.of(7, 0)));

        List<CoachingCardContent> cards = generator.generate(nightShift, CaffeineSensitivity.HIGH, 20);

        CoachingCardContent lightExposure = cardOf(cards, CoachingCardType.LIGHT_EXPOSURE);
        assertEquals(LocalDateTime.of(2026, 8, 7, 21, 0), lightExposure.windowStart());
        assertEquals(LocalDateTime.of(2026, 8, 7, 23, 0), lightExposure.windowEnd());

        CoachingCardContent nap = cardOf(cards, CoachingCardType.NAP);
        assertEquals(LocalDateTime.of(2026, 8, 7, 18, 0), nap.windowStart());
        assertEquals(LocalDateTime.of(2026, 8, 7, 18, 20), nap.windowEnd());

        CoachingCardContent caffeineCutoff = cardOf(cards, CoachingCardType.CAFFEINE_CUTOFF);
        assertEquals(LocalDateTime.of(2026, 8, 8, 0, 0), caffeineCutoff.windowStart());
        assertEquals(LocalDateTime.of(2026, 8, 8, 2, 0), caffeineCutoff.windowEnd());
    }

    @Test
    void OFF_근무는_카드를_생성하지_않는다() {
        ShiftSchedule offShift = new ShiftSchedule(
                1L, LocalDate.of(2026, 8, 7), ShiftType.OFF, new ShiftTime(null, null));

        List<CoachingCardContent> cards = generator.generate(offShift, CaffeineSensitivity.MEDIUM, 20);

        assertTrue(cards.isEmpty());
    }

    private CoachingCardContent cardOf(List<CoachingCardContent> cards, CoachingCardType type) {
        return cards.stream().filter(card -> card.cardType() == type).findFirst().orElseThrow();
    }
}
