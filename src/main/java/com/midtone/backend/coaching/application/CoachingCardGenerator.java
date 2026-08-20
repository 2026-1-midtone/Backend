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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class CoachingCardGenerator {

    private static final Duration LIGHT_EXPOSURE_MARGIN = Duration.ofHours(1);
    // 이상적 낮잠 시간대(13~16시)를 쓸 수 없을 때의 대체 구간. 교대근무자의 야간 근무 전 낮잠은
    // 근무 시작 2~3시간 전에 자는 것이 피로 완화에 가장 효과적이라는 연구(CDC 근무시간 교육자료;
    // Signal 등 교대근무 낮잠 연구)에 근거해, 근무 시작 3시간 전~2시간 전을 대체 구간으로 잡는다.
    private static final Duration NAP_LEAD_TIME_MAX = Duration.ofHours(3);
    private static final Duration NAP_LEAD_TIME_MIN = Duration.ofHours(2);
    /**
     * 취침 전 카페인 컷오프 여유시간(민감도별). 카페인 반감기가 개인마다 크게 다르다는 점을 반영한 근사치로,
     * 아래 세 근거를 종합해 설계했다(단일 논문이 이 세 수치를 직접 제시한 것은 아님):
     * - IOM(2001, NBK223808): 건강한 성인의 평균 혈장 반감기는 약 5시간(개인차 1.5~9.5시간).
     * - CYP1A2 유전 다형성(rs762551): 빠른 대사형 반감기 4~6시간, 느린 대사형 8~12시간.
     * - Gardiner et al.(2023, Sleep Medicine Reviews) 메타분석: 표준 용량(107mg) 기준 총수면시간 감소를
     *   피하려면 취침 최소 8.8시간 전 카페인 섭취를 권장.
     * LOW=빠른 대사형 반감기 하한(4h), MEDIUM=IOM 평균 반감기에 여유를 더한 값(6h),
     * HIGH=Gardiner(2023) 권장 컷오프에 근접하고 느린 대사형 범위와도 겹치는 값(9h).
     */
    private static final Map<CaffeineSensitivity, Duration> CAFFEINE_SLEEP_BUFFER = Map.of(
            CaffeineSensitivity.LOW, Duration.ofHours(4),
            CaffeineSensitivity.MEDIUM, Duration.ofHours(6),
            CaffeineSensitivity.HIGH, Duration.ofHours(9));

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
        List<CoachingCardContent> cards = new ArrayList<>();
        cards.add(lightExposureCard(shiftStart, sleepPattern));
        CoachingCardContent nap = napCard(shiftStart, preferredNapMinutes, sleepPattern);
        if (nap != null) {
            cards.add(nap);
        }
        cards.add(caffeineCutoffCard(todayShift.getWorkDate(), shiftEnd, caffeineSensitivity, sleepPattern, caffeineStatus));
        return List.copyOf(cards);
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
                    "생체리듬 최저점 이후 밝은 빛을 쬐면 위상을 앞당기는 데 도움이 돼요. 이 구간 동안 밖에서 활동하시면 좋습니다.",
                    rationale);
        }
        String rationale = "오늘 근무 시작 시각(" + shiftStart.format(DateTimeDefaults.HOUR_MINUTE) + ") 기준 앞뒤 1시간을 빛 노출 권장 시간으로 계산했습니다. (수면기록이 쌓이면 더 정확해져요)";
        return new CoachingCardContent(
                CoachingCardType.LIGHT_EXPOSURE, "밝은 빛 노출",
                shiftStart.minus(LIGHT_EXPOSURE_MARGIN), shiftStart.plus(LIGHT_EXPOSURE_MARGIN),
                "근무 시작 전후로 밝은 빛을 쬐면 각성 유지에 도움이 돼요. 이 구간 동안 밖에서 활동하시면 좋습니다.",
                rationale);
    }

    /**
     * 낮잠 카드를 계산한다. 20분 정도의 짧은 낮잠 자체는 후보 구간(13~16시 또는 근무 2~3시간 전) 안
     * 아무 때나 자면 되므로, 특정 시각으로 고정하지 않고 "이 구간 안에서 자면 된다"는 폭 있는 창으로 보여준다.
     * 두 후보 구간 모두 실제 습관적 본수면 시간대와 겹치면(이미 자고 있는 중이라 낮잠으로 안내할 의미가 없으면)
     * 카드를 아예 생성하지 않는다(null 반환).
     */
    private CoachingCardContent napCard(LocalDateTime shiftStart, int preferredNapMinutes, SleepPattern sleepPattern) {
        LocalDate date = shiftStart.toLocalDate();
        LocalDateTime defaultWindowStart = shiftStart.minus(NAP_LEAD_TIME_MAX);
        LocalDateTime defaultWindowEnd = shiftStart.minus(NAP_LEAD_TIME_MIN);
        boolean defaultOverlapsSleep = windowOverlapsHabitualSleep(defaultWindowStart, defaultWindowEnd, sleepPattern);

        LocalDateTime idealWindowStart = date.atTime(IDEAL_NAP_WINDOW_START);
        LocalDateTime idealWindowBound = date.atTime(IDEAL_NAP_WINDOW_END);
        LocalDateTime shiftBuffer = shiftStart.minus(IDEAL_NAP_MIN_BUFFER_BEFORE_SHIFT);
        LocalDateTime idealWindowEndCheck = idealWindowStart.plusMinutes(preferredNapMinutes);
        boolean idealFitsWithinRange = !idealWindowEndCheck.isAfter(idealWindowBound);
        boolean idealEndsBeforeShift = !idealWindowEndCheck.isAfter(shiftBuffer);
        LocalDateTime idealWindowEnd = idealWindowBound.isBefore(shiftBuffer) ? idealWindowBound : shiftBuffer;
        // 13~16시가 실제 습관적 본수면 시간대(예: 나이트 근무 후 낮잠이 아니라 본수면 중인 경우)와 겹치면
        // 이상적 구간을 쓰지 않고 근무 2~3시간 전 구간으로 대체한다.
        boolean idealOverlapsSleep = windowOverlapsHabitualSleep(idealWindowStart, idealWindowEnd, sleepPattern);
        boolean idealUsable = idealFitsWithinRange && idealEndsBeforeShift && !idealOverlapsSleep;

        if (idealUsable) {
            String description = "낮잠은 13~16시 사이에 " + preferredNapMinutes + "분 자는 게 가장 효과적입니다.";
            String rationale = "생체리듬상 낮잠에 가장 효과적인 13~16시 구간(김현주·기도형, 2016) 안에서 " + preferredNapMinutes
                    + "분 정도 낮잠을 자면 됩니다.";
            return new CoachingCardContent(
                    CoachingCardType.NAP, "권장 낮잠", idealWindowStart, idealWindowEnd, description, rationale);
        }
        if (!defaultOverlapsSleep) {
            String description = "낮잠은 근무 시작 2~3시간 전에 " + preferredNapMinutes + "분 자는 게 효과적입니다.";
            String rationale = idealOverlapsSleep
                    ? "13~16시가 최근 습관적 본수면 시간대와 겹쳐 있어, 야간 근무 전 낮잠은 근무 시작 2~3시간 전에 자는 것이 "
                            + "피로 완화에 가장 효과적이라는 연구에 근거해 이 구간 안에서 " + preferredNapMinutes + "분 정도 낮잠을 자면 됩니다."
                    : "13~16시 구간이 근무 시작과 겹쳐 쓸 수 없어, 야간 근무 전 낮잠은 근무 시작 2~3시간 전에 자는 것이 "
                            + "피로 완화에 가장 효과적이라는 연구에 근거해 이 구간 안에서 " + preferredNapMinutes + "분 정도 낮잠을 자면 됩니다.";
            return new CoachingCardContent(
                    CoachingCardType.NAP, "권장 낮잠", defaultWindowStart, defaultWindowEnd, description, rationale);
        }
        // 이상적 구간·대체 구간 모두 실제 습관적 본수면 시간대와 겹쳐 낮잠을 넣을 자리가 없다 — 카드를 생략한다.
        return null;
    }

    /**
     * 주어진 낮잠 후보 구간(windowStart~windowEnd)이 사용자의 습관적 본수면 시간대(취침~기상,
     * 자정을 넘나드는 경우 포함)와 겹치는지 확인한다.
     */
    private boolean windowOverlapsHabitualSleep(LocalDateTime windowStart, LocalDateTime windowEnd, SleepPattern sleepPattern) {
        if (sleepPattern == null || sleepPattern.habitualBedtime() == null || sleepPattern.habitualWakeTime() == null) {
            return false;
        }
        LocalDate date = windowStart.toLocalDate();
        LocalDateTime bedtime = date.atTime(sleepPattern.habitualBedtime());
        LocalDateTime wake = date.atTime(sleepPattern.habitualWakeTime());
        if (!wake.isAfter(bedtime)) {
            wake = wake.plusDays(1);
        }
        return overlaps(windowStart, windowEnd, bedtime, wake) || overlaps(windowStart, windowEnd, bedtime.minusDays(1), wake.minusDays(1))
                || overlaps(windowStart, windowEnd, bedtime.plusDays(1), wake.plusDays(1));
    }

    private boolean overlaps(LocalDateTime aStart, LocalDateTime aEnd, LocalDateTime bStart, LocalDateTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    private CoachingCardContent caffeineCutoffCard(
            LocalDate workDate, LocalDateTime shiftEnd, CaffeineSensitivity sensitivity,
            SleepPattern sleepPattern, DailyCaffeineStatus caffeineStatus) {
        LocalDateTime targetBedtime = resolveTargetBedtime(workDate, shiftEnd, sleepPattern);
        boolean anchoredToHabitualBedtime = hasHabitualBedtime(sleepPattern);
        Duration buffer = CAFFEINE_SLEEP_BUFFER.get(sensitivity);
        // 카페인 컷오프는 "이 구간에서만 피하면 된다"는 범위가 아니라, 이 시각 이후로는 계속 피해야 하는
        // 단일 기준점(cutoff)이다. windowStart == windowEnd로 두어 하나의 시각으로 표현한다.
        LocalDateTime cutoff = targetBedtime.minus(buffer);
        LocalDateTime windowStart = cutoff;
        LocalDateTime windowEnd = cutoff;

        StringBuilder description = new StringBuilder()
                .append(cutoff.format(DateTimeDefaults.HOUR_MINUTE))
                .append(" 이후 카페인 중단");
        if (caffeineStatus != null && caffeineStatus.overDailyLimit()) {
            description.append(" · 오늘 카페인 ").append(caffeineStatus.totalAmountMg())
                    .append("mg으로 일일 권장 상한(300mg)을 초과했어요");
        }

        String anchorText = anchoredToHabitualBedtime
                ? "최근 " + sleepPattern.sampleCount() + "일 습관적 취침시각(" + targetBedtime.format(DateTimeDefaults.HOUR_MINUTE) + ")"
                : "근무 종료 후 목표 취침 시각(" + targetBedtime.format(DateTimeDefaults.HOUR_MINUTE) + ")";
        String rationale = anchorText + "을 기준으로, 설정된 카페인 민감도(" + sensitivity
                + ") 기준 여유를 " + buffer.toHours() + "시간으로 잡아 계산된 컷오프 시각입니다. 이 시각 이후로는 취침 전까지 계속 카페인을 피하는 것이 좋습니다.";
        if (!anchoredToHabitualBedtime) {
            // 오늘과 같은 근무 유형 뒤 수면 기록이 부족해(SleepPatternCalculator) 습관적 취침시각을 못 구하고,
            // 근무 종료 시각을 취침시각으로 대략 가정해 계산했다는 걸 밝힌다.
            rationale += " 아직 이 근무 유형 뒤 수면 기록이 부족해 근무 종료 시각을 취침시각으로 대략 가정해 계산했어요. "
                    + "수면 기록을 더 남겨주시면 실제 취침시각 기준으로 더 정확하게 계산돼요.";
        }
        if (caffeineStatus != null && caffeineStatus.overDailyLimit()) {
            rationale += " 하루 300mg 이상 섭취는 불안·불쾌감을 유발하고 수면시간·수면효율에 영향을 준다는 "
                    + "국내 연구(김혜성·이종은, 2020; Lee 등, 2007 재인용) 근거가 있어 함께 안내했어요.";
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
