package com.midtone.backend.coaching.application;

import com.midtone.backend.caffeine.application.DailyCaffeineStatus;
import com.midtone.backend.coaching.domain.CoachingCard.CoachingCardContent;
import com.midtone.backend.coaching.domain.CoachingCardType;
import com.midtone.backend.global.time.DateTimeDefaults;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.sleep.application.SleepPattern;
import com.midtone.backend.user.domain.CaffeineSensitivity;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class CoachingCardGenerator {

    private static final Duration LIGHT_EXPOSURE_MARGIN = Duration.ofHours(1);
    private static final Duration NAP_LEAD_TIME = Duration.ofHours(4);
    private static final Duration CAFFEINE_CUTOFF_MARGIN = Duration.ofHours(1);
    private static final Map<CaffeineSensitivity, Duration> CAFFEINE_SLEEP_BUFFER = Map.of(
            CaffeineSensitivity.LOW, Duration.ofHours(3),
            CaffeineSensitivity.MEDIUM, Duration.ofHours(5),
            CaffeineSensitivity.HIGH, Duration.ofHours(6));

    // 낮잠: 김현주·기도형(2016) 등 국내 근거 — 13~16시가 생체리듬(circadian dip)상 가장 효과적인 낮잠 시간대.
    private static final LocalTime IDEAL_NAP_WINDOW_START = LocalTime.of(13, 0);
    private static final LocalTime IDEAL_NAP_WINDOW_END = LocalTime.of(16, 0);
    private static final Duration IDEAL_NAP_MIN_BUFFER_BEFORE_SHIFT = Duration.ofHours(1);

    // 빛노출: Peak time(CBTmin)은 통상 습관적 기상시각의 1~2시간 전(한선정·주은연, 2008)이라는 임상 리뷰 근사치.
    private static final int CBT_MIN_BEFORE_WAKE_HOURS = 2;

    public List<CoachingCardContent> generate(
            ShiftSchedule todayShift, CaffeineSensitivity caffeineSensitivity, int preferredNapMinutes) {
        return generate(todayShift, caffeineSensitivity, preferredNapMinutes, null, null);
    }

    public List<CoachingCardContent> generate(
            ShiftSchedule todayShift, CaffeineSensitivity caffeineSensitivity, int preferredNapMinutes,
            SleepPattern sleepPattern, DailyCaffeineStatus caffeineStatus) {
        boolean hasShiftTimes = todayShift.getStartTime() != null && todayShift.getEndTime() != null;
        if (!hasShiftTimes) {
            // OFF일: 근무 앵커가 없어 빛노출/낮잠은 계산 불가하지만, 습관적 취침시각이 있으면 카페인 컷오프는 계산 가능.
            if (hasHabitualBedtime(sleepPattern)) {
                return List.of(caffeineCutoffCard(
                        todayShift.getWorkDate(), null, caffeineSensitivity, sleepPattern, caffeineStatus));
            }
            return List.of();
        }
        LocalDateTime shiftStart = todayShift.getWorkDate().atTime(todayShift.getStartTime());
        LocalDateTime shiftEnd = shiftEndDateTime(todayShift);
        return List.of(
                lightExposureCard(shiftStart, sleepPattern),
                napCard(shiftStart, preferredNapMinutes),
                caffeineCutoffCard(todayShift.getWorkDate(), shiftEnd, caffeineSensitivity, sleepPattern, caffeineStatus));
    }

    private boolean hasHabitualBedtime(SleepPattern sleepPattern) {
        return sleepPattern != null && sleepPattern.habitualBedtime() != null;
    }

    private LocalDateTime shiftEndDateTime(ShiftSchedule shift) {
        LocalDateTime start = shift.getWorkDate().atTime(shift.getStartTime());
        LocalDateTime end = shift.getWorkDate().atTime(shift.getEndTime());
        return end.isBefore(start) ? end.plusDays(1) : end;
    }

    private CoachingCardContent lightExposureCard(LocalDateTime shiftStart, SleepPattern sleepPattern) {
        if (sleepPattern != null && sleepPattern.estimatedCbtMin() != null) {
            LocalDateTime cbtMin = nearestOccurrence(shiftStart, sleepPattern.estimatedCbtMin());
            LocalDateTime windowStart = cbtMin;
            LocalDateTime windowEnd = cbtMin.plus(LIGHT_EXPOSURE_MARGIN.multipliedBy(2));
            String rationale = "최근 " + sleepPattern.sampleCount() + "일 수면기록 기반 습관적 기상시각("
                    + sleepPattern.habitualWakeTime().format(DateTimeDefaults.HOUR_MINUTE) + ") - " + CBT_MIN_BEFORE_WAKE_HOURS
                    + "시간을 생체리듬 최저점(CBTmin) 추정치(" + cbtMin.format(DateTimeDefaults.HOUR_MINUTE)
                    + ")로 잡고, 그 이후를 빛 노출 권장 구간으로 계산했습니다.";
            return new CoachingCardContent(
                    CoachingCardType.LIGHT_EXPOSURE, "밝은 빛 노출", windowStart, windowEnd,
                    "생체리듬 최저점 이후 밝은 빛을 쬐면 위상을 앞당기는 데 도움이 돼요.", rationale);
        }
        String rationale = "오늘 근무 시작 시각(" + shiftStart.format(DateTimeDefaults.HOUR_MINUTE) + ") 기준 앞뒤 1시간을 빛 노출 권장 시간으로 계산했습니다. (수면기록이 쌓이면 더 정확해져요)";
        return new CoachingCardContent(
                CoachingCardType.LIGHT_EXPOSURE, "밝은 빛 노출",
                shiftStart.minus(LIGHT_EXPOSURE_MARGIN), shiftStart.plus(LIGHT_EXPOSURE_MARGIN),
                "근무 시작 전후로 밝은 빛을 쬐면 각성 유지에 도움이 돼요.", rationale);
    }

    private CoachingCardContent napCard(LocalDateTime shiftStart, int preferredNapMinutes) {
        LocalDateTime defaultWindowStart = shiftStart.minus(NAP_LEAD_TIME);
        LocalDateTime defaultWindowEnd = defaultWindowStart.plusMinutes(preferredNapMinutes);

        LocalDateTime idealWindowStart = shiftStart.toLocalDate().atTime(IDEAL_NAP_WINDOW_START);
        LocalDateTime idealWindowEnd = idealWindowStart.plusMinutes(preferredNapMinutes);
        LocalDateTime idealWindowBound = shiftStart.toLocalDate().atTime(IDEAL_NAP_WINDOW_END);
        boolean idealFitsWithinRange = !idealWindowEnd.isAfter(idealWindowBound);
        boolean idealEndsBeforeShift = !idealWindowEnd.isAfter(shiftStart.minus(IDEAL_NAP_MIN_BUFFER_BEFORE_SHIFT));

        LocalDateTime windowStart;
        LocalDateTime windowEnd;
        String rationale;
        if (idealFitsWithinRange && idealEndsBeforeShift) {
            windowStart = idealWindowStart;
            windowEnd = idealWindowEnd;
            rationale = "생체리듬상 낮잠에 가장 효과적인 13~16시 구간(김현주·기도형, 2016)에 맞춰 " + preferredNapMinutes + "분 낮잠 창을 계산했습니다.";
        } else {
            windowStart = defaultWindowStart;
            windowEnd = defaultWindowEnd;
            rationale = "근무 시작 4시간 전부터 설정된 선호 낮잠 시간(" + preferredNapMinutes + "분)만큼 낮잠 창을 계산했습니다. (13~16시 구간과 겹치지 않아 근무 시작 기준으로 계산)";
        }
        return new CoachingCardContent(
                CoachingCardType.NAP, "권장 낮잠", windowStart, windowEnd,
                "근무 전 " + preferredNapMinutes + "분 파워냅", rationale);
    }

    private CoachingCardContent caffeineCutoffCard(
            LocalDate workDate, LocalDateTime shiftEnd, CaffeineSensitivity sensitivity,
            SleepPattern sleepPattern, DailyCaffeineStatus caffeineStatus) {
        LocalDateTime targetBedtime = resolveTargetBedtime(workDate, shiftEnd, sleepPattern);
        boolean anchoredToHabitualBedtime = hasHabitualBedtime(sleepPattern);
        Duration buffer = CAFFEINE_SLEEP_BUFFER.get(sensitivity);
        LocalDateTime center = targetBedtime.minus(buffer);
        LocalDateTime windowStart = center.minus(CAFFEINE_CUTOFF_MARGIN);
        LocalDateTime windowEnd = center.plus(CAFFEINE_CUTOFF_MARGIN);

        StringBuilder description = new StringBuilder()
                .append(windowStart.format(DateTimeDefaults.HOUR_MINUTE))
                .append("~")
                .append(windowEnd.format(DateTimeDefaults.HOUR_MINUTE))
                .append(" 사이 카페인 중단");
        if (caffeineStatus != null && caffeineStatus.overDailyLimit()) {
            description.append(" · 오늘 카페인 ").append(caffeineStatus.totalAmountMg())
                    .append("mg으로 일일 상한(").append(caffeineStatus.dailyLimitMg()).append("mg)을 초과했어요");
        }

        String anchorText = anchoredToHabitualBedtime
                ? "최근 " + sleepPattern.sampleCount() + "일 습관적 취침시각(" + targetBedtime.format(DateTimeDefaults.HOUR_MINUTE) + ")"
                : "근무 종료 후 목표 취침 시각(" + targetBedtime.format(DateTimeDefaults.HOUR_MINUTE) + ")";
        String rationale = anchorText + "을 기준으로, 설정된 카페인 민감도(" + sensitivity
                + ") 기준 여유를 " + buffer.toHours() + "시간으로 잡아 계산된 창입니다.";
        if (caffeineStatus != null && caffeineStatus.overDailyLimit()) {
            rationale += " 설정하신 일일 카페인 상한(" + caffeineStatus.dailyLimitMg()
                    + "mg) 초과 섭취는 수면의 질 저하와 관련된 국내 실측연구(김혜성·이종은, 2020) 근거가 있어 함께 안내했어요.";
        }
        return new CoachingCardContent(CoachingCardType.CAFFEINE_CUTOFF, "카페인 컷오프", windowStart, windowEnd,
                description.toString(), rationale);
    }

    private LocalDateTime resolveTargetBedtime(LocalDate workDate, LocalDateTime shiftEnd, SleepPattern sleepPattern) {
        if (hasHabitualBedtime(sleepPattern)) {
            LocalDateTime reference = shiftEnd != null ? shiftEnd : workDate.atTime(12, 0);
            return nearestOccurrence(reference, sleepPattern.habitualBedtime());
        }
        return shiftEnd;
    }

    /**
     * 시각(time-of-day)만 있는 값(습관적 취침시각·CBTmin 등)을, 기준 시점(reference)에서 가장 가까운
     * 실제 날짜와 결합해 LocalDateTime으로 만든다. 자정을 넘나드는 습관(예: 새벽 취침)도 자연스럽게 처리된다.
     */
    private LocalDateTime nearestOccurrence(LocalDateTime reference, LocalTime timeOfDay) {
        LocalDateTime sameDay = reference.toLocalDate().atTime(timeOfDay);
        return Stream.of(sameDay.minusDays(1), sameDay, sameDay.plusDays(1))
                .min(Comparator.comparing(candidate -> Duration.between(candidate, reference).abs()))
                .orElse(sameDay);
    }
}
