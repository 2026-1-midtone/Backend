package com.midtone.backend.coaching.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.midtone.backend.caffeine.application.DailyCaffeineStatus;
import com.midtone.backend.coaching.domain.CoachingCard.CoachingCardContent;
import com.midtone.backend.coaching.domain.CoachingCardType;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
import com.midtone.backend.sleep.application.SleepPattern;
import com.midtone.backend.user.domain.CaffeineSensitivity;
import java.math.BigDecimal;
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

        // 이상적 낮잠 시간대(13~16시, 김현주·기도형 2016)가 근무 시작(22:00) 1시간 전 안에 들어오므로 이를 우선하고,
        // 그 구간 전체(13:00~16:00)를 "이 안에서 20분 자면 되는" 창으로 보여준다.
        CoachingCardContent nap = cardOf(cards, CoachingCardType.NAP);
        assertEquals(LocalDateTime.of(2026, 8, 7, 13, 0), nap.windowStart());
        assertEquals(LocalDateTime.of(2026, 8, 7, 16, 0), nap.windowEnd());
        assertTrue(nap.description().contains("13~16시"));

        CoachingCardContent caffeineCutoff = cardOf(cards, CoachingCardType.CAFFEINE_CUTOFF);
        assertEquals(LocalDateTime.of(2026, 8, 7, 22, 0), caffeineCutoff.windowStart());
        assertEquals(LocalDateTime.of(2026, 8, 7, 22, 0), caffeineCutoff.windowEnd());
        assertTrue(caffeineCutoff.description().contains("22:00 이후 카페인 중단"));
    }

    @Test
    void 이상적_낮잠_시간대가_근무시작과_겹치면_근무_2에서_3시간_전_구간으로_대체한다() {
        ShiftSchedule dayShift = new ShiftSchedule(
                1L, LocalDate.of(2026, 8, 7), ShiftType.DAY,
                new ShiftTime(LocalTime.of(14, 0), LocalTime.of(22, 0)));

        List<CoachingCardContent> cards = generator.generate(dayShift, CaffeineSensitivity.MEDIUM, 20);

        // 13~16시 구간은 근무 시작(14:00) 1시간 전 버퍼(13:00)와 겹쳐 쓸 수 없으므로,
        // 근무 시작 3시간 전(11:00)~2시간 전(12:00) 구간으로 대체된다.
        CoachingCardContent nap = cardOf(cards, CoachingCardType.NAP);
        assertEquals(LocalDateTime.of(2026, 8, 7, 11, 0), nap.windowStart());
        assertEquals(LocalDateTime.of(2026, 8, 7, 12, 0), nap.windowEnd());
        assertTrue(nap.description().contains("2~3시간 전"));
    }

    @Test
    void 습관적_본수면_시간대가_13에서_16시와_겹치면_근무_2에서_3시간_전_구간으로_대체한다() {
        ShiftSchedule nightShift = new ShiftSchedule(
                1L, LocalDate.of(2026, 8, 7), ShiftType.NIGHT,
                new ShiftTime(LocalTime.of(22, 0), LocalTime.of(7, 0)));
        // 습관적 본수면 09:00~15:00 - 13~16시와 겹침
        SleepPattern sleepPattern = new SleepPattern(
                LocalTime.of(9, 0), LocalTime.of(15, 0), LocalTime.of(12, 0), LocalTime.of(13, 0),
                10, LocalDate.of(2026, 7, 24), LocalDate.of(2026, 8, 6));

        List<CoachingCardContent> cards = generator.generate(
                nightShift, CaffeineSensitivity.MEDIUM, 20, sleepPattern, null);

        CoachingCardContent nap = cardOf(cards, CoachingCardType.NAP);
        assertEquals(LocalDateTime.of(2026, 8, 7, 19, 0), nap.windowStart());
        assertEquals(LocalDateTime.of(2026, 8, 7, 20, 0), nap.windowEnd());
        assertTrue(nap.description().contains("2~3시간 전"));
        assertTrue(nap.rationale().contains("본수면"));
    }

    @Test
    void 이상적_구간과_대체_구간_모두_습관적_본수면과_겹치면_낮잠_카드를_생성하지_않는다() {
        ShiftSchedule nightShift = new ShiftSchedule(
                1L, LocalDate.of(2026, 8, 7), ShiftType.NIGHT,
                new ShiftTime(LocalTime.of(22, 0), LocalTime.of(7, 0)));
        // 습관적 본수면 12:00~21:00 - 13~16시(이상적 구간)와 19~20시(근무 3~2시간 전 대체 구간) 모두와 겹침
        SleepPattern sleepPattern = new SleepPattern(
                LocalTime.of(12, 0), LocalTime.of(21, 0), LocalTime.of(16, 30), LocalTime.of(19, 0),
                10, LocalDate.of(2026, 7, 24), LocalDate.of(2026, 8, 6));

        List<CoachingCardContent> cards = generator.generate(
                nightShift, CaffeineSensitivity.MEDIUM, 20, sleepPattern, null);

        assertTrue(cards.stream().noneMatch(card -> card.cardType() == CoachingCardType.NAP));
    }

    @Test
    void OFF_근무는_카드를_생성하지_않는다() {
        ShiftSchedule offShift = new ShiftSchedule(
                1L, LocalDate.of(2026, 8, 7), ShiftType.OFF, new ShiftTime(null, null));

        List<CoachingCardContent> cards = generator.generate(offShift, CaffeineSensitivity.MEDIUM, 20);

        assertTrue(cards.isEmpty());
    }

    @Test
    void 수면패턴이_있으면_CBTmin_기준으로_빛노출_카드를_계산한다() {
        ShiftSchedule nightShift = new ShiftSchedule(
                1L, LocalDate.of(2026, 8, 7), ShiftType.NIGHT,
                new ShiftTime(LocalTime.of(22, 0), LocalTime.of(7, 0)));
        // 습관적 기상시각 15:00 - 2시간 = CBTmin 13:00 (한선정·주은연, 2008 근사치)
        SleepPattern sleepPattern = new SleepPattern(
                LocalTime.of(7, 0), LocalTime.of(15, 0), LocalTime.of(11, 0), LocalTime.of(13, 0),
                10, LocalDate.of(2026, 7, 24), LocalDate.of(2026, 8, 6));

        List<CoachingCardContent> cards = generator.generate(
                nightShift, CaffeineSensitivity.HIGH, 20, sleepPattern, null);

        CoachingCardContent lightExposure = cardOf(cards, CoachingCardType.LIGHT_EXPOSURE);
        assertEquals(LocalDateTime.of(2026, 8, 7, 13, 0), lightExposure.windowStart());
        assertEquals(LocalDateTime.of(2026, 8, 7, 15, 0), lightExposure.windowEnd());
        assertTrue(lightExposure.rationale().contains("CBTmin"));
    }

    @Test
    void 습관적_취침시각이_있으면_그것을_기준으로_카페인_컷오프를_계산한다() {
        ShiftSchedule nightShift = new ShiftSchedule(
                1L, LocalDate.of(2026, 8, 7), ShiftType.NIGHT,
                new ShiftTime(LocalTime.of(22, 0), LocalTime.of(7, 0)));
        SleepPattern sleepPattern = new SleepPattern(
                LocalTime.of(7, 30), LocalTime.of(15, 0), LocalTime.of(11, 15), LocalTime.of(13, 0),
                10, LocalDate.of(2026, 7, 24), LocalDate.of(2026, 8, 6));

        List<CoachingCardContent> cards = generator.generate(
                nightShift, CaffeineSensitivity.MEDIUM, 20, sleepPattern, null);

        // 습관적 취침 07:30(다음날) - 카페인 민감도 MEDIUM 여유 6시간 = 컷오프 01:30
        CoachingCardContent caffeineCutoff = cardOf(cards, CoachingCardType.CAFFEINE_CUTOFF);
        assertEquals(LocalDateTime.of(2026, 8, 8, 1, 30), caffeineCutoff.windowStart());
        assertEquals(LocalDateTime.of(2026, 8, 8, 1, 30), caffeineCutoff.windowEnd());
        assertTrue(caffeineCutoff.rationale().contains("습관적 취침시각"));
    }

    @Test
    void OFF_근무여도_습관적_취침시각이_있으면_카페인_컷오프_카드만_생성한다() {
        ShiftSchedule offShift = new ShiftSchedule(
                1L, LocalDate.of(2026, 8, 7), ShiftType.OFF, new ShiftTime(null, null));
        SleepPattern sleepPattern = new SleepPattern(
                LocalTime.of(23, 0), LocalTime.of(7, 0), LocalTime.of(3, 0), LocalTime.of(5, 0),
                14, LocalDate.of(2026, 7, 24), LocalDate.of(2026, 8, 6));
        DailyCaffeineStatus overLimit =
                new DailyCaffeineStatus(LocalDate.of(2026, 8, 7), 320, new BigDecimal("3.50"), true);

        List<CoachingCardContent> cards = generator.generate(
                offShift, CaffeineSensitivity.LOW, 20, sleepPattern, overLimit);

        assertEquals(1, cards.size());
        CoachingCardContent caffeineCutoff = cardOf(cards, CoachingCardType.CAFFEINE_CUTOFF);
        assertEquals(LocalDateTime.of(2026, 8, 7, 19, 0), caffeineCutoff.windowStart());
        assertEquals(LocalDateTime.of(2026, 8, 7, 19, 0), caffeineCutoff.windowEnd());
        assertTrue(caffeineCutoff.description().contains("초과"));
        assertTrue(caffeineCutoff.rationale().contains("김혜성"));
    }

    @Test
    void 카페인_섭취가_상한_이내면_경고문구가_없다() {
        ShiftSchedule nightShift = new ShiftSchedule(
                1L, LocalDate.of(2026, 8, 7), ShiftType.NIGHT,
                new ShiftTime(LocalTime.of(22, 0), LocalTime.of(7, 0)));
        DailyCaffeineStatus withinLimit =
                new DailyCaffeineStatus(LocalDate.of(2026, 8, 7), 80, new BigDecimal("1.00"), false);

        List<CoachingCardContent> cards = generator.generate(
                nightShift, CaffeineSensitivity.HIGH, 20, null, withinLimit);

        CoachingCardContent caffeineCutoff = cardOf(cards, CoachingCardType.CAFFEINE_CUTOFF);
        assertFalse(caffeineCutoff.description().contains("초과"));
    }

    private CoachingCardContent cardOf(List<CoachingCardContent> cards, CoachingCardType type) {
        return cards.stream().filter(card -> card.cardType() == type).findFirst().orElseThrow();
    }
}
