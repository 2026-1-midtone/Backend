package com.midtone.backend.coaching.application;

import com.midtone.backend.coaching.domain.CoachingCard.CoachingCardContent;
import com.midtone.backend.coaching.domain.CoachingCardType;
import com.midtone.backend.global.time.DateTimeDefaults;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.user.domain.CaffeineSensitivity;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

    public List<CoachingCardContent> generate(
            ShiftSchedule todayShift, CaffeineSensitivity caffeineSensitivity, int preferredNapMinutes) {
        if (todayShift.getStartTime() == null || todayShift.getEndTime() == null) {
            return List.of();
        }
        LocalDateTime shiftStart = todayShift.getWorkDate().atTime(todayShift.getStartTime());
        LocalDateTime shiftEnd = shiftEndDateTime(todayShift);
        return List.of(
                lightExposureCard(shiftStart),
                napCard(shiftStart, preferredNapMinutes),
                caffeineCutoffCard(shiftEnd, caffeineSensitivity));
    }

    private LocalDateTime shiftEndDateTime(ShiftSchedule shift) {
        LocalDateTime start = shift.getWorkDate().atTime(shift.getStartTime());
        LocalDateTime end = shift.getWorkDate().atTime(shift.getEndTime());
        return end.isBefore(start) ? end.plusDays(1) : end;
    }

    private CoachingCardContent lightExposureCard(LocalDateTime shiftStart) {
        String rationale = "오늘 근무 시작 시각(" + shiftStart.format(DateTimeDefaults.HOUR_MINUTE) + ") 기준 앞뒤 1시간을 빛 노출 권장 시간으로 계산했습니다.";
        return new CoachingCardContent(
                CoachingCardType.LIGHT_EXPOSURE, "밝은 빛 노출",
                shiftStart.minus(LIGHT_EXPOSURE_MARGIN), shiftStart.plus(LIGHT_EXPOSURE_MARGIN),
                "근무 시작 전후로 밝은 빛을 쬐면 각성 유지에 도움이 돼요.", rationale);
    }

    private CoachingCardContent napCard(LocalDateTime shiftStart, int preferredNapMinutes) {
        LocalDateTime windowStart = shiftStart.minus(NAP_LEAD_TIME);
        LocalDateTime windowEnd = windowStart.plusMinutes(preferredNapMinutes);
        String rationale = "근무 시작 4시간 전부터 설정된 선호 낮잠 시간(" + preferredNapMinutes + "분)만큼 낮잠 창을 계산했습니다.";
        return new CoachingCardContent(
                CoachingCardType.NAP, "권장 낮잠", windowStart, windowEnd,
                "근무 전 " + preferredNapMinutes + "분 파워냅", rationale);
    }

    private CoachingCardContent caffeineCutoffCard(LocalDateTime shiftEnd, CaffeineSensitivity sensitivity) {
        Duration buffer = CAFFEINE_SLEEP_BUFFER.get(sensitivity);
        LocalDateTime center = shiftEnd.minus(buffer);
        LocalDateTime windowStart = center.minus(CAFFEINE_CUTOFF_MARGIN);
        LocalDateTime windowEnd = center.plus(CAFFEINE_CUTOFF_MARGIN);
        String description = windowStart.format(DateTimeDefaults.HOUR_MINUTE) + "~" + windowEnd.format(DateTimeDefaults.HOUR_MINUTE) + " 사이 카페인 중단";
        String rationale = "근무 종료 후 목표 취침 시각이 " + shiftEnd.format(DateTimeDefaults.HOUR_MINUTE) + "이고, 설정된 카페인 민감도(" + sensitivity
                + ") 기준 여유를 " + buffer.toHours() + "시간으로 잡아 계산된 창입니다.";
        return new CoachingCardContent(CoachingCardType.CAFFEINE_CUTOFF, "카페인 컷오프", windowStart, windowEnd, description, rationale);
    }
}
