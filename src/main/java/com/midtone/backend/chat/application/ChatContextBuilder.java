package com.midtone.backend.chat.application;

import com.midtone.backend.shift.application.schedule.TransitionDetector;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftScheduleWindow;
import com.midtone.backend.shift.domain.ShiftType;
import com.midtone.backend.user.domain.UserSettings;
import com.midtone.backend.user.domain.UserSettingsRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ChatContextBuilder {

    private final ShiftScheduleRepository shiftScheduleRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final TransitionDetector transitionDetector;

    public ChatContextBuilder(
            ShiftScheduleRepository shiftScheduleRepository,
            UserSettingsRepository userSettingsRepository,
            TransitionDetector transitionDetector) {
        this.shiftScheduleRepository = shiftScheduleRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.transitionDetector = transitionDetector;
    }

    public String build(long userId, LocalDate today) {
        StringBuilder context = new StringBuilder("[사용자 근무 컨텍스트]\n");
        Optional<ShiftSchedule> todayShift = shiftScheduleRepository.findByUserIdAndWorkDate(userId, today);
        appendTodayShift(context, today, todayShift);
        todayShift.ifPresent(shift -> appendTransition(context, userId, today, shift.getShiftType()));
        appendNextShift(context, userId, today);
        appendSettings(context, userId);
        return context.toString();
    }

    private void appendTodayShift(StringBuilder context, LocalDate today, Optional<ShiftSchedule> todayShift) {
        if (todayShift.isEmpty()) {
            context.append("- 오늘(").append(today).append(") 등록된 근무 일정이 없습니다.\n");
            return;
        }
        ShiftSchedule shift = todayShift.get();
        context.append("- 오늘(").append(today).append(") 근무: ").append(shift.getShiftType().name());
        if (shift.getStartTime() != null && shift.getEndTime() != null) {
            context.append(" (").append(shift.getStartTime()).append("~").append(shift.getEndTime()).append(")");
        }
        context.append("\n");
    }

    private void appendTransition(StringBuilder context, long userId, LocalDate today, ShiftType todayType) {
        transitionDetector.detectTransition(userId, today, todayType).ifPresent(info -> context
                .append("- 오늘은 ").append(info.fromShiftType().name()).append(" → ")
                .append(info.toShiftType().name()).append(" 전환일입니다.\n"));
    }

    private void appendNextShift(StringBuilder context, long userId, LocalDate today) {
        List<ShiftSchedule> upcoming = shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                userId, today.plusDays(1), today.plusDays(ShiftScheduleWindow.SCAN_DAYS));
        upcoming.stream()
                .filter(shift -> shift.getShiftType() != ShiftType.OFF)
                .findFirst()
                .ifPresent(shift -> {
                    context.append("- 다음 근무: ").append(shift.getWorkDate()).append(" ")
                            .append(shift.getShiftType().name());
                    if (shift.getStartTime() != null) {
                        context.append(" ").append(shift.getStartTime()).append(" 시작");
                    }
                    context.append("\n");
                });
    }

    private void appendSettings(StringBuilder context, long userId) {
        UserSettings settings = userSettingsRepository.findById(userId).orElse(null);
        int napMinutes = settings == null
                ? UserSettings.DEFAULT_PREFERRED_NAP_MINUTES
                : settings.getPreferredNapMinutes();
        context.append("- 선호 낮잠 시간: ").append(napMinutes).append("분\n");
    }
}
