package com.midtone.backend.home.application;

import com.midtone.backend.global.time.DateTimeDefaults;
import com.midtone.backend.shift.application.schedule.CompletenessResponse;
import com.midtone.backend.shift.application.schedule.ShiftCompletenessCalculator;
import com.midtone.backend.shift.application.schedule.TransitionDetector;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftScheduleWindow;
import com.midtone.backend.shift.domain.ShiftType;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class HomeScheduleSectionBuilder {

    private static final int MIN_SCHEDULE_WEEKS = 4;
    private static final String ALERT_TYPE_NO_SCHEDULE = "NO_SCHEDULE";
    private static final String ALERT_TYPE_INSUFFICIENT_SCHEDULE = "INSUFFICIENT_SCHEDULE";

    private final ShiftScheduleRepository shiftScheduleRepository;
    private final TransitionDetector transitionDetector;
    private final ShiftCompletenessCalculator shiftCompletenessCalculator;
    private final Clock clock;

    public HomeScheduleSectionBuilder(
            ShiftScheduleRepository shiftScheduleRepository,
            TransitionDetector transitionDetector,
            ShiftCompletenessCalculator shiftCompletenessCalculator,
            Clock clock) {
        this.shiftScheduleRepository = shiftScheduleRepository;
        this.transitionDetector = transitionDetector;
        this.shiftCompletenessCalculator = shiftCompletenessCalculator;
        this.clock = clock;
    }

    public ScheduleSection build(long userId, LocalDate date) {
        Optional<ShiftSchedule> todayShift = shiftScheduleRepository.findByUserIdAndWorkDate(userId, date);
        return new ScheduleSection(
                todayShift.map(this::toTodayShift).orElse(null),
                findNextShift(userId, date),
                isTransitionDay(userId, date, todayShift),
                buildScheduleAlert());
    }

    private boolean isTransitionDay(long userId, LocalDate date, Optional<ShiftSchedule> todayShift) {
        return todayShift
                .flatMap(shift -> transitionDetector.detectTransition(userId, date, shift.getShiftType()))
                .isPresent();
    }

    private HomeDashboardResponse.TodayShift toTodayShift(ShiftSchedule shift) {
        return new HomeDashboardResponse.TodayShift(
                shift.getShiftType().name(), formatTime(shift.getStartTime()), formatTime(shift.getEndTime()));
    }

    /**
     * 아직 시작하지 않은 가장 가까운 근무. 오늘 근무도 시작 전이면 후보에 넣는다.
     * 저녁 출근을 앞둔 아침에 "다음 근무"가 비어 보이면 안 되기 때문이다.
     */
    private HomeDashboardResponse.NextShift findNextShift(long userId, LocalDate date) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<ShiftSchedule> upcoming = shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                userId, date, date.plusDays(ShiftScheduleWindow.SCAN_DAYS));
        return upcoming.stream()
                .filter(this::isSchedulableShift)
                .filter(shift -> shift.getWorkDate().atTime(shift.getStartTime()).isAfter(now))
                .findFirst()
                .map(this::toNextShift)
                .orElse(null);
    }

    private boolean isSchedulableShift(ShiftSchedule shift) {
        return shift.getShiftType() != ShiftType.OFF && shift.getStartTime() != null;
    }

    private HomeDashboardResponse.NextShift toNextShift(ShiftSchedule shift) {
        long startsInMinutes = minutesUntil(shift.getWorkDate().atTime(shift.getStartTime()));
        return new HomeDashboardResponse.NextShift(
                shift.getWorkDate().toString(), shift.getShiftType().name(), startsInMinutes);
    }

    private long minutesUntil(LocalDateTime startAt) {
        return Math.max(0, Duration.between(LocalDateTime.now(clock), startAt).toMinutes());
    }

    private HomeDashboardResponse.ScheduleAlert buildScheduleAlert() {
        CompletenessResponse completeness = shiftCompletenessCalculator.calculate(MIN_SCHEDULE_WEEKS);
        if (completeness.confirmedDays() == 0) {
            return new HomeDashboardResponse.ScheduleAlert(ALERT_TYPE_NO_SCHEDULE, completeness.requiredDays());
        }
        if (completeness.remainingDays() > 0) {
            return new HomeDashboardResponse.ScheduleAlert(ALERT_TYPE_INSUFFICIENT_SCHEDULE, completeness.remainingDays());
        }
        return null;
    }

    private String formatTime(LocalTime time) {
        return time == null ? null : time.format(DateTimeDefaults.HOUR_MINUTE);
    }

    public record ScheduleSection(
            HomeDashboardResponse.TodayShift todayShift,
            HomeDashboardResponse.NextShift nextShift,
            boolean transitionDay,
            HomeDashboardResponse.ScheduleAlert scheduleAlert) {
    }
}
