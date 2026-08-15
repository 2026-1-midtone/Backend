package com.midtone.backend.home.application;

import com.midtone.backend.shift.application.schedule.CompletenessResponse;
import com.midtone.backend.shift.application.schedule.ShiftService;
import com.midtone.backend.shift.application.schedule.TransitionDetector;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftType;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class HomeScheduleSectionBuilder {

    private static final int NEXT_SHIFT_LOOKAHEAD_DAYS = 14;
    private static final int MIN_SCHEDULE_WEEKS = 4;
    private static final String ALERT_TYPE_NO_SCHEDULE = "NO_SCHEDULE";
    private static final String ALERT_TYPE_INSUFFICIENT_SCHEDULE = "INSUFFICIENT_SCHEDULE";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final ShiftScheduleRepository shiftScheduleRepository;
    private final TransitionDetector transitionDetector;
    private final ShiftService shiftService;

    public HomeScheduleSectionBuilder(
            ShiftScheduleRepository shiftScheduleRepository,
            TransitionDetector transitionDetector,
            ShiftService shiftService) {
        this.shiftScheduleRepository = shiftScheduleRepository;
        this.transitionDetector = transitionDetector;
        this.shiftService = shiftService;
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

    private HomeDashboardResponse.NextShift findNextShift(long userId, LocalDate date) {
        List<ShiftSchedule> upcoming = shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                userId, date.plusDays(1), date.plusDays(NEXT_SHIFT_LOOKAHEAD_DAYS));
        return upcoming.stream()
                .filter(this::isSchedulableShift)
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
        return Math.max(0, Duration.between(LocalDateTime.now(), startAt).toMinutes());
    }

    private HomeDashboardResponse.ScheduleAlert buildScheduleAlert() {
        CompletenessResponse completeness = shiftService.getCompleteness(MIN_SCHEDULE_WEEKS);
        if (completeness.confirmedDays() == 0) {
            return new HomeDashboardResponse.ScheduleAlert(ALERT_TYPE_NO_SCHEDULE, completeness.requiredDays());
        }
        if (completeness.remainingDays() > 0) {
            return new HomeDashboardResponse.ScheduleAlert(ALERT_TYPE_INSUFFICIENT_SCHEDULE, completeness.remainingDays());
        }
        return null;
    }

    private String formatTime(LocalTime time) {
        return time == null ? null : time.format(TIME_FORMAT);
    }

    public record ScheduleSection(
            HomeDashboardResponse.TodayShift todayShift,
            HomeDashboardResponse.NextShift nextShift,
            boolean transitionDay,
            HomeDashboardResponse.ScheduleAlert scheduleAlert) {
    }
}
