package com.midtone.backend.home.application;

import com.midtone.backend.global.time.DateTimeDefaults;
import com.midtone.backend.shift.application.schedule.CompletenessResponse;
import com.midtone.backend.shift.application.schedule.NextShiftFinder;
import com.midtone.backend.shift.application.schedule.ShiftCompletenessCalculator;
import com.midtone.backend.shift.application.schedule.TransitionDetector;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private final NextShiftFinder nextShiftFinder;
    private final Clock clock;

    public HomeScheduleSectionBuilder(
            ShiftScheduleRepository shiftScheduleRepository,
            TransitionDetector transitionDetector,
            ShiftCompletenessCalculator shiftCompletenessCalculator,
            NextShiftFinder nextShiftFinder,
            Clock clock) {
        this.shiftScheduleRepository = shiftScheduleRepository;
        this.transitionDetector = transitionDetector;
        this.shiftCompletenessCalculator = shiftCompletenessCalculator;
        this.nextShiftFinder = nextShiftFinder;
        this.clock = clock;
    }

    public ScheduleSection build(long userId, LocalDate date) {
        Optional<ShiftSchedule> todayShift = shiftScheduleRepository.findByUserIdAndWorkDate(userId, date);
        return new ScheduleSection(
                todayShift.map(this::toTodayShift).orElse(null),
                nextShiftFinder.find(userId, date).map(this::toNextShift).orElse(null),
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

    private HomeDashboardResponse.NextShift toNextShift(ShiftSchedule shift) {
        long startsInMinutes = minutesUntil(NextShiftFinder.startAtOf(shift));
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
