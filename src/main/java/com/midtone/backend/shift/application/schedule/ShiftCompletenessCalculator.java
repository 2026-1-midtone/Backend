package com.midtone.backend.shift.application.schedule;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.shift.application.ShiftException;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftScheduleWindow;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ShiftCompletenessCalculator {

    private final ShiftScheduleRepository shiftScheduleRepository;
    private final CurrentUserIdProvider currentUserIdProvider;

    public ShiftCompletenessCalculator(
            ShiftScheduleRepository shiftScheduleRepository, CurrentUserIdProvider currentUserIdProvider) {
        this.shiftScheduleRepository = shiftScheduleRepository;
        this.currentUserIdProvider = currentUserIdProvider;
    }

    @Transactional(readOnly = true)
    public CompletenessResponse calculate(int weeks) {
        validateWeeks(weeks);
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays((long) weeks * ShiftScheduleWindow.DAYS_PER_WEEK - 1);
        return calculate(from, to);
    }

    private void validateWeeks(int weeks) {
        if (weeks < ShiftScheduleWindow.MIN_COMPLETENESS_WEEKS
                || weeks > ShiftScheduleWindow.MAX_COMPLETENESS_WEEKS) {
            throw new ShiftException(ShiftException.ErrorCode.INVALID_COMPLETENESS_WEEKS);
        }
    }

    @Transactional(readOnly = true)
    public CompletenessResponse calculate(LocalDate from, LocalDate to) {
        long userId = currentUserIdProvider.getCurrentUserId();
        List<ShiftSchedule> shifts =
                shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(userId, from, to);
        Set<LocalDate> confirmedDates = shifts.stream()
                .filter(ShiftSchedule::isConfirmed)
                .map(ShiftSchedule::getWorkDate)
                .collect(Collectors.toSet());
        List<LocalDate> allDates = from.datesUntil(to.plusDays(1)).toList();
        List<LocalDate> missingDates = allDates.stream().filter(date -> !confirmedDates.contains(date)).toList();
        return CompletenessResponse.of(allDates.size(), confirmedDates.size(), missingDates);
    }
}
